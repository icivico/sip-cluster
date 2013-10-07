package com.iccapps.sipserver.action;

public class Reject extends Action {
	
	private static final long serialVersionUID = 4170120262145319359L;
	
	private int code;
	
	public Reject(String d, int c) {
		super(d);
		code = c;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
}
