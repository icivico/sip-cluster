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
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.mobicents.ha.javax.sip.ClusteredSipStack;

import com.iccapps.sipserver.cluster.hz.ClusterImpl;

public class WaitAck extends State {
	
	public WaitAck(SessionImpl c) {
		super(c);
	}

	@Override
	public void processRequest(Request request, ServerTransaction st) {
		if (request.getMethod().equals(Request.ACK)) {
			// clear transaction
			chan.setTransaction(null);
			st.getDialog().setApplicationData(null);
			
			// check canceled
			if ( !chan.isCanceled() ) {
				// transition to linked state
				chan.setState(new Linked(chan));
				chan.replicate();
				
				chan.fireConnected();
			}
			
		} else if (request.getMethod().equals(Request.CANCEL)) {
			
			chan.setCanceled(true);
	        chan.replicate();
	        
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
		log.debug("Answer on " + chan.getDialogId());
		ServerTransaction st = getServerTransaction();
		if (st == null) return;
		
		try {
			// accept the call
	        Response okResponse = msgFactory.createResponse(Response.OK, st.getRequest());
	        ContactHeader contactHeader = headerFactory.createContactHeader(contact);
	        okResponse.addHeader(contactHeader);
	        
	        // handle SDP
	        if (chan.getLocalSDP() != null) {
	        	ContentTypeHeader cth = headerFactory.createContentTypeHeader("application", "sdp");
	        	okResponse.setContent(chan.getLocalSDP(), cth);
	        }
			
	        // send response
	        st.sendResponse(okResponse);
	        log.warn("Sent 200 OK " + chan.getDialogId());
	        
		} catch(ParseException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void keepalive() {
		log.debug("Keepalive on " + chan.getDialogId());
		ServerTransaction st = getServerTransaction();
		if (st == null) return;
		
		try {
			// 183
	        Response progressResponse = msgFactory.createResponse(Response.SESSION_PROGRESS, st.getRequest());
	        ContactHeader contactHeader = headerFactory.createContactHeader(contact);
	        progressResponse.addHeader(contactHeader);
	        String tag = st.getDialog().getLocalTag();
	        if (tag == null) 
	        	tag = ""+rnd.nextLong();
	        ((ToHeader) progressResponse.getHeader(ToHeader.NAME)).setTag(tag);
	        // if dialog is restored in early state, we need to set affinity in a progress response
	        Header keepalive = headerFactory.createHeader("X-Balancer", "affinity");
	        progressResponse.addHeader(keepalive);
	        
	        // send response
	        st.sendResponse(progressResponse);
	        log.warn("Sent 183 OK " + chan.getDialogId());
	        log.debug("\n"+progressResponse.toString()+"\n");
	        
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
