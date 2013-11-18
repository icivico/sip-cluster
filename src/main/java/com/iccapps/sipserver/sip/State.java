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

import java.util.List;
import java.util.Random;

import javax.sip.ClientTransaction;
import javax.sip.ServerTransaction;
import javax.sip.SipProvider;
import javax.sip.address.Address;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.mobicents.ha.javax.sip.ClusteredSipStack;

import com.iccapps.sipserver.cluster.hz.ClusterImpl;

public abstract class State {
	protected Logger log = Logger.getLogger(State.class);
	protected SessionImpl chan;
	
	protected MessageFactory msgFactory;
	protected HeaderFactory headerFactory;
	protected ClusteredSipStack stack;
	protected SipProvider provider;
	protected Address contact;
	protected List<String> userAgent;
	protected Random rnd;
	
	public State(SessionImpl c) {
		chan = c;
		log.debug("State transition to " + this.getClass().getSimpleName() + " - " + chan.getDialogId());
		
		Endpoint ep = ClusterImpl.getInstance().getSipEndpoint();
		msgFactory = ep.getMessageFactory();
		headerFactory = ep.getHeaderFactory();
		stack = (ClusteredSipStack)ep.getSipStack();
		provider = ep.getSipProvider();
		contact = ep.getContact();
		userAgent = ep.getUserAgent();
		rnd = ep.getRnd();
	}
	
	public abstract void processRequest(Request event, ServerTransaction st);
	public abstract void processResponse(Response event, ClientTransaction ct);
	
	protected void answer() {};
	protected void hangup() {};
	protected void reject(int code) {};
	protected void message(String text) {};
	protected void keepalive() {};
	protected void update() {};
}
