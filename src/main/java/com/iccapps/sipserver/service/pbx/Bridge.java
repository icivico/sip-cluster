/* sip-cluster - a sip clustered application based on mobicents jain-sip.ha
 	and hazelcast backend. 

    Copyright (C) 2013-2014 Iñaki Cívico Campos.

    This file is part of sip-cluster.

    sip-cluster is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    sip-cluster is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with sip-cluster. If not, see <http://www.gnu.org/licenses/>.*/

package com.iccapps.sipserver.service.pbx;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Bridge implements Serializable {

	private static final long serialVersionUID = 568013907730180534L;
	
	private String uuid;
	private String uuidStr;
	private String originationLeg;
	private String destinationLeg;
	
	private List<String> inProgressLegs = new ArrayList<String>();
	
	public Bridge() {
		uuid = UUID.randomUUID().toString();
		uuidStr = uuid.substring(0, 5) + "...";
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

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
	public List<String> getInProgressLegs() {
		return inProgressLegs;
	}
	
	public String toString() {
		return uuidStr;
	}
}
