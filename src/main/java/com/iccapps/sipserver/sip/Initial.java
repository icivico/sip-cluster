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

import gov.nist.javax.sip.stack.SIPClientTransaction;
import gov.nist.javax.sip.stack.SIPServerTransaction;

import java.text.ParseException;
import java.util.ArrayList;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.mobicents.ha.javax.sip.ClusteredSipStack;

public class Initial extends State {
	
	public Initial(SessionImpl c) {
		super(c);
	}

	@Override
	public void processRequest(Request request, ServerTransaction st) {
		
		if ( request.getMethod().equals(Request.INVITE) ) {
			chan.setTransaction(st);
			FromHeader fromHeader = (FromHeader)request.getHeader(FromHeader.NAME);
			chan.setOriginURI(fromHeader.getAddress().getURI().toString());
			ToHeader toHeader = (ToHeader)request.getHeader(ToHeader.NAME);
			chan.setDestinationURI(toHeader.getAddress().getURI().toString());
			chan.setRemoteSDP(new String(request.getRawContent()));
	        try {
	        	// send trying
	        	Response response = msgFactory.createResponse(Response.TRYING, request);
	            st.sendResponse(response);
	            
	        	// send ringing message and create dialog with to tag
	            response = msgFactory.createResponse(Response.SESSION_PROGRESS, request);
	            ToHeader to = (ToHeader) response.getHeader(ToHeader.NAME);
		        to.setTag(""+rnd.nextLong()); // Application is supposed to set.
		        
		        String txId = ((SIPServerTransaction)st).getTransactionId();
		        Dialog d = st.getDialog();
		        d.setApplicationData(txId);
		        st.sendResponse(response);
		        log.debug("TransactionId " + txId + " on dialog " + d.getDialogId());
		        	            
	            // change state
	            chan.setDialogId(st.getDialog().getDialogId());
	            chan.setState(new WaitAck(chan));
	            
	        } catch (ParseException e) {
				e.printStackTrace();
				
			} catch (TransactionAlreadyExistsException e) {
				e.printStackTrace();
				
			} catch (TransactionUnavailableException e) {
				e.printStackTrace();
				
			} catch (SipException e) {
				e.printStackTrace();
				
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
				
			}
		}
	}

	@Override
	public void processResponse(Response response, ClientTransaction ct) {
		//log.debug("Call #" + chan.getDialogId() + " received response " + response.getStatusCode() + " on dialog " + ct.getDialog());
		if ( ct.getRequest().getMethod().equals(Request.INVITE) ) {
			if ( response.getStatusCode() == Response.RINGING || 
					response.getStatusCode() == Response.SESSION_PROGRESS ) {
				log.debug("Received 180/183 on " + chan.getDialogId());
				chan.fireProceeding();
				
			} else if ( response.getStatusCode() == Response.OK ) {
				log.debug("Received 200 on " + chan.getDialogId());
				// process SDP
				chan.setRemoteSDP(new String(response.getRawContent()));
				chan.setState(new Linked(chan));
				
				sendAck(response);
				
				chan.replicate();
				
				// connected event
				chan.fireConnected();
				
			} else if ( response.getStatusCode() == Response.FORBIDDEN ) {
				log.debug("Received 403 on " + chan.getDialogId());
				sendAck(response);
				chan.fireDisconnected();
				
			} else if ( response.getStatusCode() == Response.REQUEST_TERMINATED) {
				log.debug("Received 487 on " + chan.getDialogId());
				//sendAck(response);
				chan.fireDisconnected();
			}
		}
	}
	
	private void sendAck(Response res) {
		try {
			Dialog d = ((ClusteredSipStack)stack).getDialog(chan.getDialogId());
			Request ack = d.createAck(((CSeqHeader) res.getHeader(CSeqHeader.NAME)).getSeqNumber());
			d.sendAck(ack);
			
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void hangup() {
		ClientTransaction ct = (ClientTransaction)getClientTransaction();
		
		try {
			// cancel
	        Request cancelReq = ct.createCancel();
	        ContactHeader contactHeader = headerFactory.createContactHeader(contact);
	        cancelReq.addHeader(contactHeader);
	        
	        chan.setCanceled(true);
	        
	        // send response
	        provider.sendRequest(cancelReq);
	        
		} catch (SipException e) {
			e.printStackTrace();
		} 
	}
	
	public String invite(String requri, String touri, String fromuri, Address balancer, String sdp) {
		try {
			Address fromNameAddress = addressFactory.createAddress(fromuri);
			FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, ""+rnd.nextLong());
			Address toNameAddress = addressFactory.createAddress(touri);
			ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);
			
	        SipURI requestURI = (SipURI) addressFactory.createAddress(requri).getURI();
	        
	        ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
	        ViaHeader viaHeader = headerFactory.createViaHeader(
	        			provider.getListeningPoint("udp").getIPAddress(),
	        			provider.getListeningPoint("udp").getPort(),
	                    "udp",
	                    null);
	        viaHeaders.add(viaHeader);

	        CallIdHeader callIdHeader = provider.getNewCallId();
	        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader((long)1, Request.INVITE);
	        MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);
	        
	        Request invite = msgFactory.createRequest(
	                requestURI, Request.INVITE, callIdHeader, cSeqHeader,
	                fromHeader, toHeader, viaHeaders, maxForwards);
	        
	        ContactHeader contactHeader = headerFactory.createContactHeader(contact);
	        invite.addHeader(contactHeader);
	        
	        UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(userAgent);
	        invite.addHeader(userAgentHeader);
	        
	        if (balancer != null) {
	        	Address routeAddr = (Address)balancer.clone();
	        	((SipURI)routeAddr.getURI()).setLrParam();
	        	invite.addHeader(headerFactory.createRouteHeader(routeAddr));
	        }
	        
	        ContentTypeHeader cth = headerFactory.createContentTypeHeader("application", "sdp");
			invite.setContent(sdp.getBytes(), cth);
	        
	        ClientTransaction trans = provider.getNewClientTransaction(invite);
	        trans.sendRequest();
	        trans.getDialog().setApplicationData(((SIPClientTransaction)trans).getTransactionId());
	        chan.setTransaction(trans);
	        log.debug("Sent " + invite);
	        
	        return callIdHeader.getCallId();
	        
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
