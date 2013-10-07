package com.iccapps.sipserver.action;

import java.io.Serializable;

public abstract class Action implements Serializable {

	private static final long serialVersionUID = -6402058734084231785L;

	protected String dialogId;
	protected long timestamp;
	
	public Action(String d) {
		dialogId = d;
		timestamp = System.currentTimeMillis();
	}

	public String getDialogId() {
		return dialogId;
	}

	public void setDialogId(String dialogId) {
		this.dialogId = dialogId;
	}

	public long getTimestamp() {
		return timestamp;
	}
}
