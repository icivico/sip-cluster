package com.iccapps.sipserver.action;

public class OutboundCall extends Action {
	
	private static final long serialVersionUID = 2495289187795951120L;
	
	private String fromUri;
	private String toUri;
	private String sdp;
	private String reference;

	public OutboundCall(String d, String f, String t, String s, String r) {
		super(d);
		fromUri = f;
		toUri = t;
		sdp = s;
		reference = r;
	}

	public String getFromUri() {
		return fromUri;
	}

	public void setFromUri(String fromUri) {
		this.fromUri = fromUri;
	}

	public String getToUri() {
		return toUri;
	}

	public void setToUri(String toUri) {
		this.toUri = toUri;
	}

	public String getSdp() {
		return sdp;
	}

	public void setSdp(String sdp) {
		this.sdp = sdp;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

}
