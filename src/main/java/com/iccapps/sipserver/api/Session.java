package com.iccapps.sipserver.api;

public interface Session {
	
	public static final int REJECT_CODE_NOT_FOUND = 404;
	
	public void registerListener(Controller c);
	
	public String getDialogId();
	public String getReference();
	public String getLocalSDP();
	public String getRemoteSDP();
	public boolean isCanceled();
	public String getOriginationURI();
	public String getDestinationURI();
}
