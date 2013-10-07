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
