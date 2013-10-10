/* sip-cluster - a sip clustered application based on mobicents jain-sip.ha
 	and hazelcast backend. 

    Copyright (C) 2013-2014 Iñaki Cívico Campos.

    This file is part of sip-cluster.

    sip-cluster is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    sip-cluster is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with sip-cluster. If not, see <http://www.gnu.org/licenses/>.*/

package com.iccapps.sipserver;

import gov.nist.javax.sip.DialogTimeoutEvent;
import gov.nist.javax.sip.SipListenerExt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TooManyListenersException;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.TransportNotSupportedException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

import com.iccapps.sipserver.action.Action;
import com.iccapps.sipserver.action.OutboundCall;
import com.iccapps.sipserver.cluster.hz.ClusterImpl;
import com.iccapps.sipserver.exception.StackNotInitialized;
import com.iccapps.sipserver.session.SessionImpl;

public class Endpoint implements SipListenerExt {
	
	private static Logger logger = Logger.getLogger(Endpoint.class);
	private static Endpoint instance;
	
	private SipStack sipStack;
	private SipFactory sipFactory;
	protected AddressFactory addressFactory;
	protected HeaderFactory headerFactory;
	protected MessageFactory messageFactory;
	protected SipProvider sipProvider;
	protected ListeningPoint udp;
	protected Random rnd = new Random(System.currentTimeMillis());
	protected Timer timer = new Timer();
	protected Address contact;
	protected Address balancer;
	public List<String> userAgent = new ArrayList<String>();
	private List<String> server = new ArrayList<String>();
	
	private HashMap<String, SessionImpl> channels = new HashMap<String, SessionImpl>();
	private HashMap<String, SessionImpl> outbounds = new HashMap<String, SessionImpl>();
	
	private String keepaliveCallid;
	
	private class BalancerKeepalive extends TimerTask {

		@Override
		public void run() {
			
			try {
				FromHeader fromHeader = headerFactory.createFromHeader(contact, ""+rnd.nextLong());
				ToHeader toHeader = headerFactory.createToHeader(balancer, null);

		        SipURI requestURI = (SipURI) balancer.getURI();
		        requestURI.setTransportParam("udp");
		        
		        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
		        ViaHeader viaHeader = headerFactory.createViaHeader(
		        			udp.getIPAddress(),
		        			udp.getPort(),
		                    "udp",
		                    null);
		        viaHeaders.add(viaHeader);

		        CallIdHeader callIdHeader = sipProvider.getNewCallId();
		        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader((long)1, Request.OPTIONS);
		        MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
		        
		        Request options = messageFactory.createRequest(
		                requestURI, Request.OPTIONS, callIdHeader, cSeqHeader,
		                fromHeader, toHeader, viaHeaders, maxForwards);
		        
		        ContactHeader contactHeader = headerFactory.createContactHeader(contact);
		        options.addHeader(contactHeader);
		        
		        UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(userAgent);
		        options.addHeader(userAgentHeader);
		        
		        Header keepalive = headerFactory.createHeader("X-Balancer", "keepalive");
		        options.addHeader(keepalive);
		        
		        keepaliveCallid = callIdHeader.getCallId();
		        
		        ClientTransaction trans = sipProvider.getNewClientTransaction(options);
		        //logger.debug("ClientTransaction " + trans);
		        trans.sendRequest();
		        
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			} catch (TransactionUnavailableException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized static Endpoint getInstance() {
		if (instance == null)
			instance = new Endpoint();
		
		return instance;
	}
	
	private Endpoint() {
		userAgent.add("SIP Server HA Agent (1.0-SNAPSHOT)");
		server.add("SIP HA Server");
	}
	
	public void start() throws FileNotFoundException, IOException, StackNotInitialized {
		
		logger.info("Starting sip endpoint");
		
		Properties config = Server.getConfig();
		
		sipFactory = SipFactory.getInstance();
		sipFactory.setPathName(config.getProperty(Constants.JAIN_SIP_PATHNAME, "gov.nist"));
		
		Properties properties = new Properties();
		properties.load(new FileInputStream(new File("jsip.properties")));
		
		try {
			sipStack = sipFactory.createSipStack(properties);
			headerFactory = sipFactory.createHeaderFactory();
			addressFactory = sipFactory.createAddressFactory();
			messageFactory = sipFactory.createMessageFactory();
			
		} catch (PeerUnavailableException e) {
			logger.error("Creating stack", e);
			throw new StackNotInitialized("Stack not created", e);
		}
		
		
		String port = config.getProperty(Constants.JAIN_SIP_PORT, "5080");
		String ip = config.getProperty(Constants.JAIN_SIP_ADDRESS, "0.0.0.0");
		
		try {
			udp = sipStack.createListeningPoint(ip, Integer.parseInt(port), "udp");
			sipProvider = sipStack.createSipProvider(udp);
			sipProvider.addSipListener(this);
			sipStack.start();
			
		} catch (NumberFormatException e) {
			logger.error("Invalid port", e);
			throw new StackNotInitialized("Listening point not created", e);
			
		} catch (TransportNotSupportedException e) {
			logger.error("Invalid transport", e);
			throw new StackNotInitialized("Listening point not created", e);
			
		} catch (InvalidArgumentException e) {
			logger.error("Invalid argument", e);
			throw new StackNotInitialized("Listening point not created", e);
			
		} catch (ObjectInUseException e) {
			logger.error("Creating provider", e);
			throw new StackNotInitialized("Provider not created", e);
			
		} catch (TooManyListenersException e) {
			logger.error("Creating provider", e);
			throw new StackNotInitialized("Provider not created", e);
			
		} catch (SipException e) {
			logger.error("Starting stack", e);
			throw new StackNotInitialized("Stack could not be started", e);
		}
	
		try {
			contact = addressFactory.createAddress("sip:"+udp.getIPAddress()+":"+udp.getPort());
			
		} catch (ParseException e) {
			logger.error("Creating contact address", e);
			throw new StackNotInitialized("Contact address not created", e);
		}
		
		try {
			balancer = addressFactory.createAddress("sip:"+config.getProperty(Constants.BALANCER));
			
		} catch (ParseException e) {
			logger.error("Creating contact address", e);
			throw new StackNotInitialized("Contact address not created", e);
		}
		
		timer.schedule(new BalancerKeepalive(), 1000);
	}
	
	public void stop() {
		logger.info("Stopping sip endpoint");
		if (sipProvider != null)
			sipProvider.removeSipListener(this);
		
		if (sipStack != null)
			sipStack.stop();
		
		sipStack = null;
	}
	
	public void dispatch(Action a) {
		if (a.getDialogId() == null) {
			// process out of dialog action
			if (a instanceof OutboundCall) {
				OutboundCall oc = (OutboundCall)a;
				initiateCall(oc.getFromUri(), oc.getToUri(), oc.getSdp(), oc.getReference());
			}
			
		} else if (channels.containsKey(a.getDialogId())) {
			// process in-dialog action
			channels.get(a.getDialogId()).dispatch(a);
		}
	}
	
	public void recover(SessionImpl chan) {
		logger.info("Channel recovered : " + chan.getDialogId());
		channels.put(chan.getDialogId(), chan);
		// TODO - send OPTIONS
	}
	
	public SessionImpl getSession(String id) {
		return channels.get(id);
	}

	@Override
	public void processDialogTerminated(DialogTerminatedEvent dte) {
		logger.debug("Dialog terminated: " + dte.getDialog().getDialogId());
		SessionImpl c = channels.get(dte.getDialog().getDialogId());
		if (c != null) {
			c.fireTerminated();
			channels.remove(c.getDialogId());
			logger.debug("Channel removed: " + c.getDialogId());
			ClusterImpl.getInstance().unregister(c);
			ServiceManager.getInstance().stop(c);
			c.end();
		}
	}

	@Override
	public void processIOException(IOExceptionEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processRequest(RequestEvent ev) {
		Request req = ev.getRequest();
		//logger.debug("Received request: " + req.getMethod());
		try {
			ServerTransaction st = ev.getServerTransaction(); 
			if ( st != null ) {
				Dialog d = st.getDialog();
				logger.debug("Received request with transaction: " + req.getMethod());
				if ( d != null ) {
					// search channel
					if ( channels.containsKey(d.getDialogId()) ) {
						SessionImpl chan = channels.get(d.getDialogId());
						chan.getState().processRequest(req, ev.getServerTransaction());
						
					} else {
						logger.warn("Request with dialog but no call: " + d);
						/*/ ----- hack -----
						SIPCall c = new SIPCall(d);
						calls.put(d, c);
						c.setState(new Linked(c));
						c.getState().processRequest(req, ev.getServerTransaction());
						// ------ hack ------*/
					}
					
				} else {
					logger.warn("Request with server transaction but no dialog?!");
				}
				
			} else {
				if (req.getMethod().equals(Request.OPTIONS)) {
					Response res = messageFactory.createResponse(Response.OK, req);
					((ToHeader)res.getHeader(ToHeader.NAME)).setTag(""+rnd.nextLong());
					sipProvider.getNewServerTransaction(req).sendResponse(res);
					//logger.debug("Sent response: " + res.getStatusCode());
					
				} else if (req.getMethod().equals(Request.REGISTER)) {
					ToHeader to = (ToHeader) req.getHeader(ToHeader.NAME);
					SipURI uri = (SipURI)to.getAddress().getURI();
					String user = uri.getUser();
					ContactHeader ct = (ContactHeader)req.getHeader(ContactHeader.NAME);
					String contactUri = ct.getAddress().getURI().toString();
					int expires = ServiceManager.getInstance().registration(user, contactUri);
					
					Response res = null;
					if (expires > 0) {
						res = messageFactory.createResponse(Response.OK, req);
						((ToHeader)res.getHeader(ToHeader.NAME)).setTag(""+rnd.nextLong());
					
						ExpiresHeader exp = headerFactory.createExpiresHeader(expires);
						res.addHeader(exp);
					} else  {
						res = messageFactory.createResponse(Response.NOT_FOUND, req);
						((ToHeader)res.getHeader(ToHeader.NAME)).setTag(""+rnd.nextLong());
					}
					sipProvider.getNewServerTransaction(req).sendResponse(res);
					//logger.debug("Sent response: " + res.getStatusCode());
					
				} else if (req.getMethod().equals(Request.INVITE)) {
					ServerTransaction nst = sipProvider.getNewServerTransaction(req);
					// new
					SessionImpl	chan = new SessionImpl();
					// process request and obtain dialogid
					chan.getState().processRequest(req, nst);
					// add to my channels
					channels.put(chan.getDialogId(), chan);
					//logger.debug("New channel: " + chan.getDialogId());
					
					// create distributed queue for commands and add channelid to this node registry
					ClusterImpl.getInstance().register(chan);
					// register on Service
					ServiceManager.getInstance().start(chan);
					// start call and process request
					chan.init();
					chan.fireIncoming();
					
					chan.update();
				}	
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void processResponse(ResponseEvent ev) {
		Response res = ev.getResponse();
		//logger.debug("Received response: " + res);
		ClientTransaction ct = ev.getClientTransaction();
		Dialog d = (ct != null)?ct.getDialog():null; 
		if (d != null && d.getDialogId() != null) {
			// search channel
			if (channels.containsKey(d.getDialogId())) {
				SessionImpl chan = channels.get(d.getDialogId());
				chan.getState().processResponse(res, ct);
				
			} else {
				// search in outbound calls
				String callid = ((CallIdHeader)res.getHeader(CallIdHeader.NAME)).getCallId();
				if(outbounds.containsKey(callid)) {
					SessionImpl chan = outbounds.remove(callid);
					chan.setDialogId(d.getDialogId());
					channels.put(chan.getDialogId(), chan);
					ClusterImpl.getInstance().register(chan);
					ServiceManager.getInstance().start(chan);
					chan.getState().processResponse(res, ct);
					
				} else {
					logger.warn("Request with dialog but no call: " + d);
				}
			}
		} else {
			String callid = ((CallIdHeader)res.getHeader(CallIdHeader.NAME)).getCallId();
			if (ct != null && ct.getRequest() != null) {
				if (ct.getRequest().getMethod().equals(Request.OPTIONS)) {
					if (callid.equals(keepaliveCallid)) {
						//logger.debug("Balancer contacted, scheduling new keepalive");
						keepaliveCallid = "";
						timer.schedule(new BalancerKeepalive(), 1000);
					}
				}
			}
		}
	}

	@Override
	public void processTimeout(TimeoutEvent ev) {
		logger.debug("Timeout: " + ev);
		if (ev.getClientTransaction() != null) {
			Request req = ev.getClientTransaction().getRequest();
			String callid = ((CallIdHeader)req.getHeader(CallIdHeader.NAME)).getCallId();
			if (callid.equals(keepaliveCallid)) {
				logger.warn("Re-scheduling balancer keepalive");
				keepaliveCallid = "";
				timer.schedule(new BalancerKeepalive(), 100);
			}
		}
	}

	@Override
	public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processDialogTimeout(DialogTimeoutEvent arg0) {
		logger.debug("Dialog Timeout: " + arg0);

	}

	private void initiateCall(String fromuri, String touri, String sdp, String reference) {
		try {
			Address fromNameAddress = addressFactory.createAddress(fromuri);
			FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, ""+rnd.nextLong());
			Address toNameAddress = addressFactory.createAddress(touri);
			ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

	        SipURI requestURI = (SipURI) toNameAddress.getURI();//addressFactory.createAddress(arg0).createSipURI(, arg1)(touri);
	        requestURI.setTransportParam("udp");

	        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
	        ViaHeader viaHeader = headerFactory.createViaHeader(
	        			udp.getIPAddress(),
	        			udp.getPort(),
	                    "udp",
	                    null);
	        viaHeaders.add(viaHeader);

	        CallIdHeader callIdHeader = sipProvider.getNewCallId();
	        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader((long)1, Request.INVITE);
	        MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
	        
	        Request invite = messageFactory.createRequest(
	                requestURI, Request.INVITE, callIdHeader, cSeqHeader,
	                fromHeader, toHeader, viaHeaders, maxForwards);
	        
	        ContactHeader contactHeader = headerFactory.createContactHeader(contact);
	        invite.addHeader(contactHeader);
	        
	        UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(userAgent);
	        invite.addHeader(userAgentHeader);
	        
	        Address routeAddr = (Address)balancer.clone();
	        ((SipURI)routeAddr.getURI()).setLrParam();
	        invite.addHeader(headerFactory.createRouteHeader(routeAddr));
	        
	        ContentTypeHeader cth = headerFactory.createContentTypeHeader("application", "sdp");
			invite.setContent(sdp.getBytes(), cth);
	        
	        ClientTransaction trans = sipProvider.getNewClientTransaction(invite);
	        logger.debug("ClientTransaction " + trans);
	        
	        SessionImpl c = new SessionImpl();
	        c.setOriginURI(fromuri);
	        c.setDestinationURI(touri);
	        c.setLocalSDP(sdp);
	        c.getData().setReference(reference);
	        outbounds.put(callIdHeader.getCallId(), c);
	        
	        trans.sendRequest();
	        
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		}	
	}
	
	public SipStack getSipStack() {
		return sipStack;
	}

	public AddressFactory getAddressFactory() {
		return addressFactory;
	}

	public HeaderFactory getHeaderFactory() {
		return headerFactory;
	}

	public MessageFactory getMessageFactory() {
		return messageFactory;
	}

	public SipProvider getSipProvider() {
		return sipProvider;
	}

	public Address getContact() {
		return contact;
	}

	public List<String> getUserAgent() {
		return userAgent;
	}

	public Random getRnd() {
		return rnd;
	}

	public Timer getTimer() {
		return timer;
	}

}
