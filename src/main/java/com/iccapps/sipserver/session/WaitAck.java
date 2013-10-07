package com.iccapps.sipserver.session;

import java.text.ParseException;

import javax.sip.ClientTransaction;
import javax.sip.InvalidArgumentException;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.iccapps.sipserver.Endpoint;

public class WaitAck extends State {
	
	public WaitAck(SessionImpl c) {
		super(c);
	}

	@Override
	public void processRequest(Request request, ServerTransaction st) {
		if (request.getMethod().equals(Request.ACK)) {
			// clear transaction
			chan.setTransaction(null);
			// check canceled
			if ( !chan.isCanceled() ) {
				// transition to linked state
				chan.setState(new Linked(chan));
				chan.update();
				
				chan.fireConnected();
			}
			
		} else if (request.getMethod().equals(Request.CANCEL)) {
			
			chan.setCanceled(true);
	        chan.update();
	        
			// cancel invite
			try {
				ServerTransaction t = (ServerTransaction)chan.getTransaction();
				Response terminatedResponse = msgFactory.createResponse(Response.REQUEST_TERMINATED, t.getRequest());
				ContactHeader contactHeader = headerFactory.createContactHeader(contact);
				terminatedResponse.addHeader(contactHeader);
		        
		        t.sendResponse(terminatedResponse);
		        
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			}
			// response to CANCEL
			try {
				Response okResponse = msgFactory.createResponse(Response.OK, request);
				st.sendResponse(okResponse);
		        
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			}
			
			// fire disconnected event
			chan.fireDisconnected();
		}
	}
	
	@Override
	protected void answer() {
		ServerTransaction st = (ServerTransaction)chan.getTransaction();
		Request request = st.getRequest();
		
		try {
			// accept the call
	        Response okResponse = msgFactory.createResponse(Response.OK, request);
	        ContactHeader contactHeader = headerFactory.createContactHeader(contact);
	        okResponse.addHeader(contactHeader);
	        
	        // handle SDP
	        if (chan.getLocalSDP() != null) {
	        	ContentTypeHeader cth = headerFactory.createContentTypeHeader("application", "sdp");
	        	okResponse.setContent(chan.getLocalSDP(), cth);
	        }
			
	        // send response
	        st.sendResponse(okResponse);
	        
		} catch(ParseException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void hangup() {
		ServerTransaction st = (ServerTransaction)chan.getTransaction();
		Request request = st.getRequest();
		
		try {
			// reject with busy here
	        Response cancelResponse = msgFactory.createResponse(Response.BUSY_HERE, request);
	        ContactHeader contactHeader = headerFactory.createContactHeader(contact);
	        cancelResponse.addHeader(contactHeader);
	        ToHeader toHeader = (ToHeader) cancelResponse.getHeader(ToHeader.NAME);
	        toHeader.setTag(""+rnd.nextLong()); // Application is supposed to set.
	        cancelResponse.addHeader(contactHeader);
	      
	        chan.setCanceled(true);
	        
	        // send response
	        st.sendResponse(cancelResponse);
	        
		} catch(ParseException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		}
	
	}
	
	@Override
	public void reject(int code) {
		ServerTransaction st = (ServerTransaction)chan.getTransaction();
		Request request = st.getRequest();
		
		try {
			// reject
	        Response cancelResponse = msgFactory.createResponse(code, request);
	        ContactHeader contactHeader = headerFactory.createContactHeader(contact);
	        cancelResponse.addHeader(contactHeader);
	        ToHeader toHeader = (ToHeader) cancelResponse.getHeader(ToHeader.NAME);
	        toHeader.setTag(""+rnd.nextLong()); // Application is supposed to set.
	        cancelResponse.addHeader(contactHeader);
	      
	        chan.setCanceled(true);
	        
	        // send response
	        st.sendResponse(cancelResponse);
	        
		} catch(ParseException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		}
	
	}
	@Override
	public void processResponse(Response response, ClientTransaction ct) {
		// TODO Auto-generated method stub

	}

}
