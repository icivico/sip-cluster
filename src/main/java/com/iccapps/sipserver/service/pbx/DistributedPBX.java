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
	private Map<String, Object> registrations;
	private List<Object> bridges;
	private Lock bridgesLock;
	//private Timer timer = new Timer();
	private Cluster cluster;
	

	public DistributedPBX(Properties config) {}
	
	@Override
	public void initialize(Cluster c) {
		cluster = c;
		HazelcastInstance hz = Hazelcast.getHazelcastInstanceByName("jain-sip-ha");
		registrations = hz.getMap("pbx.registrar");
		bridges = hz.getList("pbx.bridges");
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
	public int registration(String user, String uri) {
		logger.info("Registration: " + user + " -> " + uri);
		registrations.put(user, uri);
		return 60;
	}

	@Override
	public void incoming(Session s) {
		// search caller
		final String caller = s.getOriginationURI();
		final String dialogId = s.getDialogId();
		final String sdp = s.getRemoteSDP();
		String called = s.getDestinationURI();
		called = called.substring(called.indexOf(':')+1, called.indexOf('@'));
		final String calledUri = (String)registrations.get(called);
		
		logger.info(caller + " wants to call " + called);
		
		if (calledUri != null) {
			Bridge b = new Bridge();
			b.setOriginationLeg(dialogId);
			bridgesLock.lock();
			try {
				logger.debug("Created new bridge " + b.toString() + " for " + dialogId);
				bridges.add(b);
				
			} finally {
				bridgesLock.unlock();
			}
			// originate call
			cluster.originate(caller, calledUri, sdp, dialogId);
			
		} else {
			cluster.doReject(dialogId, Session.REJECT_CODE_NOT_FOUND);
		}
	}

	@Override
	public void updateMedia(Session chan) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void progress(Session chan) {
		logger.info("Progress " + chan.getDialogId());
		
	}

	@Override
	public void connected(Session chan) {
		logger.info("Connected " + chan.getDialogId());
		// search bridge
		String ref = chan.getReference();
		if (ref != null) {
			// call related to another
			int idx = -1;
			Bridge b = null;
			bridgesLock.lock();
			try {
				int i = 0;
				for (Object o : bridges) {
					b = (Bridge)o;
					if (b.getOriginationLeg().equals(ref)) {
						logger.debug("Located bridge " + b);
						b.setDestinationLeg(chan.getDialogId());
						idx = i;
						break;
					}
					i++;
				}
				if (b != null) {
					// update bridge
					bridges.remove(idx);
					bridges.add(b);
					logger.debug("Updated bridge " + b);
				}
			} finally {
				bridgesLock.unlock();
			}
			if (b != null) {
				Session orig = cluster.getSession(b.getOriginationLeg());
				if (orig != null)
					cluster.doAnswer(orig.getDialogId(), chan.getRemoteSDP());
			}
		}
	}

	@Override
	public void disconnected(Session chan) {
		logger.info("Disconnected " + chan.getDialogId());
		
		bridgesLock.lock();
		try {
			for (Object o : bridges) {
				Bridge b = (Bridge)o;
				if (b.getOriginationLeg().equals(chan.getDialogId()) ||
						b.getDestinationLeg().equals(chan.getDialogId())) {
					String otherId = b.getOriginationLeg().equals(chan.getDialogId())?b.getDestinationLeg():b.getOriginationLeg();
					bridges.remove(b);
					Session other = cluster.getSession(otherId);
					cluster.doHangup(other.getDialogId());
					break;
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
			for(Object o : bridges) {
				System.out.println("-------- bridge ----------");
				System.out.println("a leg: " + ((Bridge)o).getOriginationLeg().substring(0,15) + "...");
				System.out.println("b leg: " + ((Bridge)o).getDestinationLeg().substring(0,15) + "...");
			}
			System.out.println("======================================");
		} finally {
			bridgesLock.unlock();
		}
		
	}

}
