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

package com.iccapps.sipserver.api;

public interface Session {
	
	public static final int REJECT_CODE_NOT_FOUND = 404;
	
	public void registerListener(Controller c);
	
	public String getDialogId();
	public String getReference();
	public void setReference(String ref);
	public String getLocalSDP();
	public String getRemoteSDP();
	public boolean isCanceled();
	public String getOriginationURI();
	public String getDestinationURI();
}
