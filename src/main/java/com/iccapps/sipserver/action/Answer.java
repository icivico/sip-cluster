package com.iccapps.sipserver.action;

public class Answer extends Action {

	private static final long serialVersionUID = -775390419206987283L;
	
	private String sdp;

	public Answer(String d, String s) {
		super(d);
		sdp = s;
	}

	public String getSdp() {
		return sdp;
	}

	public void setSdp(String sdp) {
		this.sdp = sdp;
	}
}
