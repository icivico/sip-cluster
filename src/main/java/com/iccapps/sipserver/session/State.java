package com.iccapps.sipserver.session;

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

import com.iccapps.sipserver.Endpoint;

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
		
		Endpoint ep = Endpoint.getInstance();
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
	protected void options() {};
	protected void update() {};
}
