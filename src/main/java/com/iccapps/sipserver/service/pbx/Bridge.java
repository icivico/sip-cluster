package com.iccapps.sipserver.service.pbx;

import java.io.Serializable;

public class Bridge implements Serializable {

	private static final long serialVersionUID = 568013907730180534L;
	
	private String originationLeg;
	private String destinationLeg;
	
	public String getOriginationLeg() {
		return originationLeg;
	}
	public void setOriginationLeg(String originLeg) {
		this.originationLeg = originLeg;
	}
	public String getDestinationLeg() {
		return destinationLeg;
	}
	public void setDestinationLeg(String destinationLeg) {
		this.destinationLeg = destinationLeg;
	}

}
