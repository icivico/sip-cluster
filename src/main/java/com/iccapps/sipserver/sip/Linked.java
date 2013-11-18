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

package com.iccapps.sipserver.sip;

import java.text.ParseException;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.Header;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.iccapps.sipserver.cluster.hz.ClusterImpl;

public class Linked extends State {
	
	public Linked(SessionImpl c) {
		super(c);
	}

	@Override
	public void processRequest(Request request, ServerTransaction st) {
		
		if (request.getMethod().equals(Request.BYE)) {
			try {
				Response res = msgFactory.createResponse(Response.OK, request);
				chan.setState(new Disconnecting(chan));
				st.sendResponse(res);
				chan.replicate();
				chan.fireDisconnected();
				
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			} 
			
		} else if (request.getMethod().equals(Request.MESSAGE)) {
			try {
				Response res = msgFactory.createResponse(Response.OK, request);
				st.sendResponse(res);
				chan.fireMessage(new String(request.getRawContent()));
				
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			} 
		} else if(request.getMethod().equals(Request.INVITE)) {
			// re-invite
			try {
				Response response = msgFactory.createResponse(Response.TRYING, request);
				st.sendResponse(response);
				
				chan.setTransaction(st);
				//chan.setRemoteSDP()
				
				chan.fireUpdateMedia();
				
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			}
			
		} else if(request.getMethod().equals(Request.ACK)) {
			chan.setTransaction(null);
		}
	}

	@Override
	public void processResponse(Response response, ClientTransaction ct) {
		if (ct.getRequest().getMethod().equals(Request.MESSAGE)) {
			if (response.getStatusCode() == 200) {
				log.debug("Message sent");
			}
		}
	}
	
	@Override
	public void hangup() {
		
		Dialog d = stack.getDialog(chan.getDialogId());
		try {
			// hangup with normal code
			Request request = d.createRequest(Request.BYE);
			//ContactHeader contactHeader = headerFactory.createContactHeader(contact);
			//request.addHeader(contactHeader);
	        ClientTransaction c = provider.getNewClientTransaction(request);
	        
			chan.setState(new Disconnecting(chan));
			// send request
	        d.sendRequest(c);
	        
	        chan.replicate();
	        
		} catch (SipException e) {
			e.printStackTrace();
		} 

	}
	
	@Override
	public void message(String text) {
		
		Dialog d = stack.getDialog(chan.getDialogId());
		try {
			Request request = d.createRequest(Request.MESSAGE);
			ContactHeader contactHeader = headerFactory.createContactHeader(contact);
			request.addHeader(contactHeader);
			
			UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(userAgent);
	        request.addHeader(userAgentHeader);
			
			ContentTypeHeader cth = headerFactory.createContentTypeHeader("text", "plain");
			request.setContent(text, cth);
			
	        ClientTransaction c = provider.getNewClientTransaction(request);
	        
	        log.debug("Sendind message: " + text);
	        
			// send request
	        d.sendRequest(c);
	        
		} catch (SipException e) {
			e.printStackTrace();
			
		} catch (ParseException e) {
			e.printStackTrace();
		} 

	}
	
	@Override
	public void keepalive() {
		Dialog d = stack.getDialog(chan.getDialogId());
		//log.debug("Send OPTIONS to " + d.getRemoteParty());
		try {
			// hangup with normal code
			Request request = d.createRequest(Request.OPTIONS);
			if (ClusterImpl.optionsOnlyToBalancer) {
				Header keepalive = headerFactory.createHeader("X-Balancer", "keepalive");
				request.addHeader(keepalive);
			}
	        ClientTransaction c = provider.getNewClientTransaction(request);
	        
			// send request
	        d.sendRequest(c);
	        
		} catch (SipException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void update() {
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
}
