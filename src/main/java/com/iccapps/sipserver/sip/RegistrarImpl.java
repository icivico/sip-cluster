package com.iccapps.sipserver.sip;

import gov.nist.javax.sip.clientauthutils.DigestServerAuthenticationHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Map;
import java.util.Properties;

import javax.sip.ClientTransaction;
import javax.sip.InvalidArgumentException;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

import com.iccapps.sipserver.api.Cluster;

public class RegistrarImpl {
	
	private static Logger logger = Logger.getLogger(RegistrarImpl.class);
	private Endpoint ep;
	private Cluster cluster;
	private Map<String, Object> registry;
	private Properties credentials;
	
	public RegistrarImpl(Endpoint e, Cluster c) throws FileNotFoundException, IOException {
		ep = e;
		ep.setRegistrar(this);
		cluster = c;
		registry = cluster.createDistributedMap("registrar.registry");
		credentials = new Properties();
		credentials.load(new FileInputStream(new File("users.properties")));
	}
	
	public void processRequest(Request req, ServerTransaction st) {
		try {
			if (st == null)
				st = ep.sipProvider.getNewServerTransaction(req);
			
			Response response = ep.messageFactory.createResponse(Response.TRYING, req);
			st.sendResponse(response);
			
			// process
			ToHeader to = (ToHeader) req.getHeader(ToHeader.NAME);
			SipURI aor = (SipURI)to.getAddress().getURI();
			String user = aor.getUser()+"@"+aor.getHost();
			
			// check user
			String expectedPass = credentials.getProperty(user);
			if (expectedPass == null) {
				// forbidden
				Response res = ep.messageFactory.createResponse(Response.NOT_FOUND, req);
				st.sendResponse(res);
				logger.info("AoR not found : " + aor.toString());
				return;
			}
			
			// Verify authorization
			try {
				DigestServerAuthenticationHelper dsam = new DigestServerAuthenticationHelper();
				if (!dsam.doAuthenticatePlainTextPassword(req, expectedPass)) {
	                Response challengeResponse = ep.messageFactory.createResponse(
	                        Response.UNAUTHORIZED, req);
	                dsam.generateChallenge(ep.headerFactory, challengeResponse, "nist.gov");
	                st.sendResponse(challengeResponse);
	                return;
	            }
				
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
            
			// store or forgive
			ContactHeader ct = (ContactHeader)req.getHeader(ContactHeader.NAME);
			String expiresParam = ct.getParameter("expires"); 
			if (expiresParam != null && expiresParam.equals("0")) {
				registry.remove(aor.toString());
				logger.info("AoR contact unregistered : " + aor.toString());
			} else {
				URI contactUri = (URI)ct.getAddress().getURI().clone();
				registry.put(aor.toString(), contactUri);
				logger.info("AoR contact registered " + aor.toString() + " -> " + contactUri.toString());
			}
			
			// response OK with no authentication
			Response res = ep.messageFactory.createResponse(Response.OK, req);
			((ToHeader)res.getHeader(ToHeader.NAME)).setTag(""+ep.rnd.nextLong());
		
			ExpiresHeader exp = ep.headerFactory.createExpiresHeader(60);
			res.addHeader(exp);
			
			Address binding = (Address)ct.getAddress().clone();
			((SipURI)binding.getURI()).setParameter("expires", ""+60);
			ContactHeader ch = ep.headerFactory.createContactHeader(binding);
			res.addHeader(ch);
				
			st.sendResponse(res);
			
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
