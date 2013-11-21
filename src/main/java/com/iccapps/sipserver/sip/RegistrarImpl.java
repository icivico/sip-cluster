package com.iccapps.sipserver.sip;

import gov.nist.javax.sip.clientauthutils.DigestServerAuthenticationHelper;
import gov.nist.javax.sip.header.From;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;

import javax.sip.ClientTransaction;
import javax.sip.InvalidArgumentException;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.DateHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MinExpiresHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

import com.iccapps.sipserver.api.Cluster;
import com.iccapps.sipserver.cluster.hz.ClusterImpl;
import com.iccapps.sipserver.db.OrientDbRegistrarProvider;
import com.iccapps.sipserver.sip.registrar.Binding;
import com.iccapps.sipserver.sip.registrar.SipUser;

public class RegistrarImpl {
	
	private static Logger logger = Logger.getLogger(RegistrarImpl.class);
	private Endpoint ep;
	private Cluster cluster;
	private Map<String, Object> registry;
	private OrientDbRegistrarProvider db;
	
	public RegistrarImpl(Endpoint e, Cluster c, Properties config) throws FileNotFoundException, IOException {
		ep = e;
		ep.setRegistrar(this);
		cluster = c;
		registry = cluster.createDistributedMap("registrar.registry");
		db = new OrientDbRegistrarProvider();
		db.configure(config);
	}
	
	public void processRequest(Request req, ServerTransaction st) {
		try {
			int expires = 60;
			
			if (st == null)
				st = ep.sipProvider.getNewServerTransaction(req);
			
			Response response = ep.messageFactory.createResponse(Response.TRYING, req);
			st.sendResponse(response);
			
			/* 10.3 Processing REGISTER Requests
			 1. The registrar inspects the Request-URI to determine whether it
	         has access to bindings for the domain identified in the
	         Request-URI.  If not, and if the server also acts as a proxy
	         server, the server SHOULD forward the request to the addressed
	         domain, following the general behavior for proxying messages
	         described in Section 16. 
			 */
			if (!req.getRequestURI().isSipURI()) {
				Response res = ep.messageFactory.createResponse(Response.UNSUPPORTED_URI_SCHEME, req);
				st.sendResponse(res);
				logger.info("Invalid Request-URI " + req.getRequestURI().toString());
				return;
			}
			String domain = ((SipURI)req.getRequestURI()).getHost();
			if (!db.isDomainSupported(domain)) {
				Response res = ep.messageFactory.createResponse(Response.NOT_ACCEPTABLE_HERE, req);
				st.sendResponse(res);
				logger.info("Domain not acceptable " + req.getRequestURI().toString());
				return;
			}
			
			/* 3. A registrar SHOULD authenticate the UAC.  Mechanisms for the
	         authentication of SIP user agents are described in Section 22.
	         Registration behavior in no way overrides the generic
	         authentication framework for SIP.  If no authentication
	         mechanism is available, the registrar MAY take the From address
	         as the asserted identity of the originator of the request.
			*/
			FromHeader from = (FromHeader) req.getHeader(FromHeader.NAME);
			SipURI fromUri = (SipURI)from.getAddress().getURI();
			SipUser u = db.findSipUser(fromUri.getUser(), fromUri.getHost());
			if (u == null) {
				Response res = ep.messageFactory.createResponse(Response.NOT_FOUND, req);
				st.sendResponse(res);
				logger.info("User not found : " + fromUri.toString());
				return;
			}
			
			try {
				DigestServerAuthenticationHelper dsam = new DigestServerAuthenticationHelper();
				if (!dsam.doAuthenticatePlainTextPassword(req, u.getPassword())) {
	                Response challengeResponse = ep.messageFactory.createResponse(
	                        Response.UNAUTHORIZED, req);
	                dsam.generateChallenge(ep.headerFactory, challengeResponse, "nist.gov");
	                st.sendResponse(challengeResponse);
	                return;
	            }
				
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			
			/*5. The registrar extracts the address-of-record from the To header
	         field of the request.  If the address-of-record is not valid
	         for the domain in the Request-URI, the registrar MUST send a
	         404 (Not Found) response and skip the remaining steps.  The URI
	         MUST then be converted to a canonical form.  To do that, all
	         URI parameters MUST be removed (including the user-param), and
	         any escaped characters MUST be converted to their unescaped
	         form.  The result serves as an index into the list of bindings.
            */
			ToHeader to = (ToHeader) req.getHeader(ToHeader.NAME);
			SipURI aorUri = (SipURI)to.getAddress().getURI();
			String aorS = "sip:"+aorUri.getUser()+"@"+aorUri.getHost(); 
			List<Binding> bindings = (List<Binding>)registry.get(aorS);
			if (bindings == null) 
				bindings = new ArrayList<Binding>();
			
			/*6. The registrar checks whether the request contains the Contact
	         header field.  If not, it skips to the last step.
	         */
			ContactHeader ct = (ContactHeader)req.getHeader(ContactHeader.NAME);
			if (ct != null) {
				/*If the Contact header field is present, the registrar checks if there
		         is one Contact field value that contains the special value "*"
		         and an Expires field.*/
				if (ct.getAddress().toString().startsWith("sip: *")) {
					/* If the request has additional Contact
			         fields or an expiration time other than zero, the request is
			         invalid, and the server MUST return a 400 (Invalid Request) and
			         skip the remaining steps. */
					while (ct.getParameterNames().hasNext()) {
						if (!ct.getParameterNames().next().toString().equalsIgnoreCase("expires")) {
							Response res = ep.messageFactory.createResponse(Response.BAD_REQUEST, req);
							st.sendResponse(res);
							logger.info("Invalid request : " + ct.toString());
							return;
						} else {
							String expiresParam = ct.getParameter("expires");
							if (expiresParam == null || !expiresParam.trim().equals("0")) {
								Response res = ep.messageFactory.createResponse(Response.BAD_REQUEST, req);
								st.sendResponse(res);
								logger.info("Invalid expires param : " + ct.toString());
								return;
							}
							// remove binding
							registry.remove(aorS);
							logger.info("AoR contact unregistered : " + aorS);
						}
					}
					/* If not, the registrar checks whether
			         the Call-ID agrees with the value stored for each binding.  If
			         not, it MUST remove the binding.  If it does agree, it MUST
			         remove the binding only if the CSeq in the request is higher
			         than the value stored for that binding.  Otherwise, the update
			         MUST be aborted and the request fails.*/
					// TODO - 
				}
				
				/* 7. The registrar now processes each contact address in the Contact
		         header field in turn.  For each address, it determines the
		         expiration interval as follows:*/
				ListIterator<ContactHeader> itr = (ListIterator<ContactHeader>)req.getHeaders(ContactHeader.NAME); 
				while(itr.hasNext()) {
					ct = itr.next();
					String contactUri = ct.getAddress().toString();
					
					/*-  If there is neither, a locally-configured default value MUST
		            be taken as the requested expiration.*/
					/*-  If the field value has an "expires" parameter, that value
		            MUST be taken as the requested expiration.*/
					String expPar = ct.getParameter("expires");
					if (expPar != null) {
						try {
							expires = Integer.parseInt(expPar);
						} catch(Exception e) { }
						
					} else {
						/*-  If there is no such parameter, but the request has an
			            Expires header field, that value MUST be taken as the
			            requested expiration.*/
						ExpiresHeader expH = (ExpiresHeader)req.getHeader(ExpiresHeader.NAME);
						expires = expH.getExpires();
					}

			        /*The registrar MAY choose an expiration less than the requested
			         expiration interval.  If and only if the requested expiration
			         interval is greater than zero AND smaller than one hour AND
			         less than a registrar-configured minimum, the registrar MAY
			         reject the registration with a response of 423 (Interval Too
			         Brief).  This response MUST contain a Min-Expires header field
			         that states the minimum expiration interval the registrar is
			         willing to honor.  It then skips the remaining steps.*/
					if(expires > 0 && expires < 60) { // registrar minimal interval 60
						MinExpiresHeader minExpiresHeader = ep.headerFactory.createMinExpiresHeader(60);
						Response res = ep.messageFactory.createResponse(Response.INTERVAL_TOO_BRIEF, req);
						res.addHeader(minExpiresHeader);
						st.sendResponse(res);
						logger.info("Interval too brief : " + expires);
						return;
					}
			         
					/*For each address, the registrar then searches the list of
			         current bindings using the URI comparison rules. */
					Binding existingBinding = null;
					for (Binding b : bindings) {
						if (b.getContact().equals(contactUri)) {
							existingBinding = b;
							break;
						}
					}
					String callid = ((CallIdHeader)req.getHeader(CallIdHeader.NAME)).getCallId();
					long cseq = ((CSeqHeader)req.getHeader(CSeqHeader.NAME)).getSeqNumber();
					if (existingBinding == null && expires > 0) {
						/* If the binding does not exist, it is tentatively added.*/
						Binding binding = new Binding();
						binding.setContact(contactUri);
						binding.setAor(aorS);
						binding.setCallid(callid);
						binding.setCseq(cseq);
						binding.setExpires(expires);
						bindings.add(binding);
						logger.info("AoR contact registered " + aorS + " -> " + contactUri);
						
					} else {
						/*If the binding does exist, the registrar checks the Call-ID value.  If
				         the Call-ID value in the existing binding differs from the
				         Call-ID value in the request, the binding MUST be removed if
				         the expiration time is zero and updated otherwise.*/
						if (!callid.equals(existingBinding.getCallid())) {
							if (expires == 0) {
								bindings.remove(existingBinding);
								logger.info("AoR contact unregistered : " + aorS);
							} else {
								existingBinding.setExpires(expires);
								existingBinding.setCallid(callid);
								existingBinding.setContact(contactUri);
								existingBinding.setCseq(cseq);
								logger.info("AoR contact registered " + aorS + " -> " + contactUri);
							}
						} else {
							/*If they are the same, the registrar compares the CSeq value.  If the value
					         is higher than that of the existing binding, it MUST update or
					         remove the binding as above.*/
							if (cseq > existingBinding.getCseq()) {
								if (expires == 0) {
									bindings.remove(existingBinding);
									logger.info("AoR contact unregistered : " + aorS);
								} else {
									existingBinding.setExpires(expires);
									existingBinding.setCallid(callid);
									existingBinding.setContact(contactUri);
									existingBinding.setCseq(cseq);
									logger.info("AoR contact registered " + aorS + " -> " + contactUri);
								}
							} else {
								/* If not, the update MUST be aborted and the request fails.*/
								Response res = ep.messageFactory.createResponse(Response.SERVER_INTERNAL_ERROR, req);
								st.sendResponse(res);
								logger.info("Request out of order : " + expires);
								return;
							}
						}
					}
				}
				
				/*The binding updates MUST be committed (that is, made visible to
		         the proxy or redirect server) if and only if all binding
		         updates and additions succeed.  If any one of them fails (for
		         example, because the back-end database commit failed), the
		         request MUST fail with a 500 (Server Error) response and all
		         tentative binding updates MUST be removed.*/
				registry.put(aorS, bindings);
			}
			
			/* 8. The registrar returns a 200 (OK) response.  The response MUST
	         contain Contact header field values enumerating all current
	         bindings.  Each Contact value MUST feature an "expires"
	         parameter indicating its expiration interval chosen by the
	         registrar.  The response SHOULD include a Date header field.*/
			Response res = ep.messageFactory.createResponse(Response.OK, req);
			((ToHeader)res.getHeader(ToHeader.NAME)).setTag(""+ep.rnd.nextLong());
		
			ExpiresHeader exp = ep.headerFactory.createExpiresHeader(expires);
			res.addHeader(exp);
			
			for(Binding b : bindings) {
				Address bindingAddr = ep.addressFactory.createAddress(b.getContact());
				((SipURI)bindingAddr.getURI()).setParameter("expires", ""+b.getExpires());
				ContactHeader ch = ep.headerFactory.createContactHeader(bindingAddr);
				res.addHeader(ch);	
			}
			
			DateHeader dateHeader = ep.headerFactory.createDateHeader(Calendar.getInstance());
			req.addHeader(dateHeader);
			
			st.sendResponse(res);
			
			logger.trace("Register response " + res.toString());
			
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		}
	}
	
	public void processResponse(Response event, ClientTransaction ct) {
		
	}
	
	public URI find(URI aor) {
		logger.debug("Find " + aor.toString());
		return (URI)registry.get(aor.toString());
	}
}
