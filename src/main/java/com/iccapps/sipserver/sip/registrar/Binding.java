package com.iccapps.sipserver.sip.registrar;

import java.io.Serializable;

public class Binding implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String aor;
	private long expires;
	private String contact;
	private String callid;
	private long cseq;
	
	public String getCallid() {
		return callid;
	}
	public void setCallid(String callid) {
		this.callid = callid;
	}
	public long getCseq() {
		return cseq;
	}
	public void setCseq(long cseq) {
		this.cseq = cseq;
	}
	public String getAor() {
		return aor;
	}
	public void setAor(String aor) {
		this.aor = aor;
	}
	public long getExpires() {
		return expires;
	}
	public void setExpires(long expires) {
		this.expires = expires;
	}
	public String getContact() {
		return contact;
	}
	public void setContact(String contact) {
		this.contact = contact;
	}

}
