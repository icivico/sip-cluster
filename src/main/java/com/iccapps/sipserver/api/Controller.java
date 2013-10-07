package com.iccapps.sipserver.api;


public interface Controller {
	
	public void incoming(Session chan);
	public void updateMedia(Session chan);
	public void progress(Session chan);
	public void connected(Session chan);
	public void disconnected(Session chan);
	public void message(Session chan, String text);
	public void dtmf(Session chan);
	public void terminated(Session chan);

}
