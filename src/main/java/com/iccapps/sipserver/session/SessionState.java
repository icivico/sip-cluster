package com.iccapps.sipserver.session;

import java.io.Serializable;

public class SessionState implements Serializable {
	
	private static final long serialVersionUID = -2184783359333382090L;
	
	private String dialogId;
	private String localSDP;
	private String remoteSDP;
	private String originURI;
	private String destinationURI;
	private boolean canceled = false;
	private String state;
	/**
	 * Reference to track originating calls
	 */
	private String reference;
	
	
	public String getDialogId() {
		return dialogId;
	}
	public void setDialogId(String dialogId) {
		this.dialogId = dialogId;
	}
	public String getLocalSDP() {
		return localSDP;
	}
	public void setLocalSDP(String localSDP) {
		this.localSDP = localSDP;
	}
	public String getRemoteSDP() {
		return remoteSDP;
	}
	public void setRemoteSDP(String remoteSDP) {
		this.remoteSDP = remoteSDP;
	}
	public String getOriginURI() {
		return originURI;
	}
	public void setOriginURI(String originURI) {
		this.originURI = originURI;
	}
	public String getDestinationURI() {
		return destinationURI;
	}
	public void setDestinationURI(String destinationURI) {
		this.destinationURI = destinationURI;
	}
	public boolean isCanceled() {
		return canceled;
	}
	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getReference() {
		return reference;
	}
	public void setReference(String reference) {
		this.reference = reference;
	}
	

}
