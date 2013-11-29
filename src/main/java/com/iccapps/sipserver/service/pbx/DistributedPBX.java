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

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.iccapps.sipserver.api.Cluster;
import com.iccapps.sipserver.api.Controller;
import com.iccapps.sipserver.api.Service;
import com.iccapps.sipserver.api.Session;

public class DistributedPBX implements Service, Controller {
	
	private static Logger logger = Logger.getLogger(DistributedPBX.class);
	private Map<String, Object> bridges;
	private Lock bridgesLock;
	private Cluster cluster;
	
	@Override
	public void configure(Properties config) {
	
	}
	
	@Override
	public void initialize(Cluster c) {
		cluster = c;
		HazelcastInstance hz = Hazelcast.getHazelcastInstanceByName("jain-sip-ha");
		bridges = hz.getMap("pbx.bridges");
		bridgesLock = hz.getLock("pbx.bridgesLock");
	}
	
	
	@Override
	public void start(Session chan) {
		chan.registerListener(this);
		
	}

	@Override
	public void stop(Session chan) {
		chan.registerListener(null);
		
	}

	@Override
	public void handover(Session chan) {
		logger.info(">>>>>>>>>> Channel handover : " + chan.getDialogId());
		chan.registerListener(this);
	}

	@Override
	public void incoming(Session s) {
		// search caller
		final String caller = s.getOriginationURI();
		final String dialogId = s.getDialogId();
		final String sdp = s.getRemoteSDP();
		String called = s.getDestinationURI();
		
		logger.info(caller + " wants to call " + called);
		
		if (called != null) {
			Bridge b = new Bridge();
			b.setOriginationLeg(dialogId);
			bridgesLock.lock();
			try {
				logger.debug("Created new bridge " + b.toString() + " for " + dialogId);
				bridges.put(b.getUuid(), b);
				
			} finally {
				bridgesLock.unlock();
			}
			// originate call
			s.setReference(b.getUuid());
			cluster.originate(caller, called, sdp, b.getUuid());
			
		} else {
			cluster.doReject(dialogId, Session.REJECT_CODE_NOT_FOUND);
		}
	}
	
	@Override
	public void destroy() {
		
	}

	@Override
	public void updateMedia(Session chan) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void progress(Session chan) {
		logger.info("Progress " + chan.getDialogId());
		String ref = chan.getReference();
		if (ref != null) {
			bridgesLock.lock();
			try {
				Bridge b = (Bridge)bridges.get(ref);
				if (b != null) {
					logger.info("Bridge " + b + ", in-progress leg " + chan.getDialogId());
					if (b.getDestinationLeg() == null) {
						// we store this leg if b leg is null
						b.getInProgressLegs().add(chan.getDialogId());
						// update bridge
						bridges.put(b.getUuid(), b);
						logger.debug("Updated bridge " + b);
						
					} else {
						// b leg is not null, hangup
						cluster.doHangup(chan.getDialogId());
					}
				}
			} finally {
				bridgesLock.unlock();
			}
		}
	}

	@Override
	public void connected(Session chan) {
		logger.info("Connected " + chan.getDialogId());
		// search bridge
		String ref = chan.getReference();
		if (ref != null) {
			bridgesLock.lock();
			try {
				Bridge b = (Bridge)bridges.get(ref);
				if (b != null) {
					logger.debug("Located bridge " + b);
					if (!chan.getDialogId().equals(b.getOriginationLeg())) {
						if (b.getDestinationLeg() == null) {
							// set as b leg if is null
							b.setDestinationLeg(chan.getDialogId());
							b.getInProgressLegs().remove(chan.getDialogId());
							// hangup any other legs
							for (String cid : b.getInProgressLegs()) {
								cluster.doHangup(cid);
							}
							b.getInProgressLegs().clear();
							// answer originating leg
							cluster.doAnswer(b.getOriginationLeg(), chan.getRemoteSDP());
							logger.info("Bridge " + b + ", legB connected " + chan.getDialogId());
							
						} else {
							// hangup 
							cluster.doHangup(chan.getDialogId());
						}
						b.getInProgressLegs().clear();
						bridges.put(b.getUuid(), b);
						logger.debug("Updated bridge " + b);
						
					} else {
						logger.info("Bridge " + b + ", successfully connected");
					}
				}
			} finally {
				bridgesLock.unlock();
			}
		}
	}

	@Override
	public void disconnected(Session chan) {
		logger.info("Disconnected " + chan.getDialogId());
		
		String ref = chan.getReference();
		
		bridgesLock.lock();
		try {
			Bridge b = (Bridge)bridges.get(ref);
			if (b != null) {
				if (b.getOriginationLeg() != null && b.getDestinationLeg() != null) {
					if (b.getOriginationLeg().equals(chan.getDialogId()) ||
							b.getDestinationLeg().equals(chan.getDialogId())) {
						// one leg disconnected, hangup the other leg
						String otherId = b.getOriginationLeg().equals(chan.getDialogId())?b.getDestinationLeg():b.getOriginationLeg();
						logger.debug("Bridge " + b + " a leg disconnected, hangup the other leg " + otherId);
						cluster.doHangup(otherId);
						bridges.remove(ref);
						logger.info("Bridge " + b + " removed");
					}
				} else if (b.getOriginationLeg() != null && b.getOriginationLeg().equals(chan.getDialogId())) {
					// bridge is in progress and originating leg disconnected, hangup all in progress legs
					for (String id : b.getInProgressLegs()) {
						cluster.doHangup(id);
					}
					b.getInProgressLegs().clear();
					bridges.remove(ref);
					logger.info("Bridge " + b + " removed");
					
				} else if (b.getOriginationLeg() != null && b.getDestinationLeg() == null && 
						b.getInProgressLegs().contains(chan.getDialogId())) {
					// bridge in progress and an in-progress leg disconnected
					logger.info("Bridge " + b + ", disconnected in progress leg " + chan.getDialogId());
					b.getInProgressLegs().remove(chan.getDialogId());
					if (b.getInProgressLegs().size() == 0) {
						// no more legs in progress, we must hangup originating leg
						logger.info("Bridge " + b + ", no more in-progress legs");
						cluster.doHangup(b.getOriginationLeg());
						bridges.remove(ref);
						logger.info("Bridge " + b + " removed");
					} else {
						logger.info("Bridge " + b + ", " + b.getInProgressLegs().size() + " in-progress legs");
						
					}
				}
			}
		} finally {
			bridgesLock.unlock();
		}
	}

	@Override
	public void message(Session chan, String text) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dtmf(Session chan) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void terminated(Session chan) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void report() {
		bridgesLock.lock();
		try {
			System.out.println("========== DistributedPBX ===========");
			for(Object o : bridges.values()) {
				Bridge b = (Bridge)o;
				String legA = b.getOriginationLeg();
				String legB = b.getDestinationLeg();
				System.out.println("-------- bridge " + b.getUuid().substring(0, 5) + "... ----------");
				System.out.println("a leg: " + ((legA != null)?(legA.substring(0,5) + "..."):"null"));
				System.out.println("b leg: " + ((legB != null)?(legB.substring(0,5) + "..."):"null"));
			}
			System.out.println("======================================");
		} finally {
			bridgesLock.unlock();
		}
		
	}

	@Override
	public void registration(String aor, boolean reg) {
		// TODO Auto-generated method stub
		
	}

}
