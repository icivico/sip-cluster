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

package com.iccapps.sipserver.cluster;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.iccapps.sipserver.session.SessionState;

public class NodeData implements Serializable {
	
	private static final long serialVersionUID = -5663872490176601033L;
	
	private String name;
	private Map<String, SessionState> channels = new HashMap<String, SessionState>();
	
	public Map<String, SessionState> getChannels() {
		return channels;
	}
	public void setChannels(Map<String, SessionState> channels) {
		this.channels = channels;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

}
