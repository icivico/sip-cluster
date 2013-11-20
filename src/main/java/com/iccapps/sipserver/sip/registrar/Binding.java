package com.iccapps.sipserver.sip.registrar;


public class Binding {
	private String aor;
	private long expires;
	private String contact;
	
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
