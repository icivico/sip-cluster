package com.iccapps.sipserver.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public interface Cluster {
	
	public Map<String, Object> createDistributedMap(String name);
	public List<Object> createDistributedList(String name);
	public Lock createDistributedLock(String name);
	public void originate(String originationUri, String destinationUri, String sdp, String reference);
	public Session getSession(String sessionId);
	
	public void doAnswer(String dialogId, String sdp);
	public void doHangup(String dialogId);
	public void doReject(String dialogId, int code);

}
