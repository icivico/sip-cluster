package com.iccapps.sipserver.cluster;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.iccapps.sipserver.session.SessionState;

public class NodeData implements Serializable {
	
	private static final long serialVersionUID = -5663872490176601033L;
	
	private String name;
	private Map<String, SessionState> channels = new HashMap<String, SessionState>();
	
	public Map<String, SessionState> getChannels() {
		return channels;
	}
	public void setChannels(Map<String, SessionState> channels) {
		this.channels = channels;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

}
