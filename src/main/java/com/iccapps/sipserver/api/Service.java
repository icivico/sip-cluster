package com.iccapps.sipserver.api;


public interface Service {
	
	public void initialize(Cluster c);
	
	public void start(Session s);
	public void stop(Session s);
	public void handover(Session s);
	public int registration(String user, String uri);
	public void report();
}
