package com.iccapps.sipserver

import com.iccapps.sipserver.api.*

public class Test implements Service, Controller {

	def cluster

	/* Service implementation */
	public void initialize(Cluster c) {
		println "Initializing groovy service"
		cluster = c
	}
	
	public void start(Session s) {
		s.registerListener(this)
	}
	public void stop(Session s) {
		s.registerListener(null)
	}
	public void handover(Session s) {
	}
	public int registration(String user, String uri) {
	}
	public void report() {
	}
	
	/* Controller implementation */
	public void incoming(Session s) {
		println "Incoming call " + s.getDialogId()
		cluster.doAnswer(s.getDialogId(), null)
	}
	public void updateMedia(Session s) {
	}
	public void progress(Session s) {
	}
	public void connected(Session s) {
	}
	public void disconnected(Session s) {
		println "Call disconnected " + s.getDialogId()
			
	}
	public void message(Session s, String text) {
	}
	public void dtmf(Session s) {
	}
	public void terminated(Session s) {
	}
} 