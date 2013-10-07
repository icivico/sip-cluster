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
