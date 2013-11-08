package com.iccapps.sipserver.service.test;

import java.io.Serializable;

public class TestCallState implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String UNINITIALIZED = "uninitialized";
	public static final String PROGRESS = "progress";
	public static final String CONNECT = "connect";
	public static final String DISCONNECT = "disconnect";
	private String id;
	private String state;
	
	public TestCallState(String id, String s) {
		this.id = id;
		this.state = s;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
}
