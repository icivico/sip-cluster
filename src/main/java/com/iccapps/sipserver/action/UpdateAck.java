package com.iccapps.sipserver.action;

public class UpdateAck extends Action {
	private static final long serialVersionUID = -6578770691319866050L;
	
	private String sdp;

	public UpdateAck(String d) {
		super(d);
	}

	public String getSdp() {
		return sdp;
	}

	public void setSdp(String sdp) {
		this.sdp = sdp;
	}
}
