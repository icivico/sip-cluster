package com.iccapps.sipserver.session;

import javax.sip.ClientTransaction;
import javax.sip.ServerTransaction;
import javax.sip.message.Request;
import javax.sip.message.Response;

public class Disconnecting extends State {
	
	public Disconnecting(SessionImpl c) {
		super(c);
	}

	@Override
	public void processRequest(Request request, ServerTransaction st) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processResponse(Response response, ClientTransaction ct) {
		if (ct.getRequest().getMethod().equals(Request.BYE)) {
			if ( response.getStatusCode() == Response.OK ) ;
				chan.fireDisconnected();
			
		} else if(ct.getRequest().getMethod().equals(Request.CANCEL)) {
			if ( response.getStatusCode() == Response.OK ) ;
				chan.fireDisconnected();
		}
	}

}
