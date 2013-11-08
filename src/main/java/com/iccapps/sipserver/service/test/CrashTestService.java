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

package com.iccapps.sipserver.service.test;

import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.iccapps.sipserver.action.UpdateAck;
import com.iccapps.sipserver.api.Cluster;
import com.iccapps.sipserver.api.Controller;
import com.iccapps.sipserver.api.Service;
import com.iccapps.sipserver.api.Session;
import com.iccapps.sipserver.cluster.hz.ClusterImpl;

public class CrashTestService implements Service, Controller {
	
	private static Logger logger = Logger.getLogger(CrashTestService.class);
	
	private Timer timer = new Timer();
	private Cluster cluster;
	private Map<String, Object> calls;
	
	public CrashTestService() { }
	
	@Override
	public void configure(Properties config) { }
	
	@Override
	public void initialize(Cluster c) {
		cluster = c;
		calls = cluster.createDistributedMap("testservice.calls");
	}
	
	@Override
	public void destroy() {
		
		
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
	public void incoming(Session s) {
		logger.info("Incoming : " + s.getDialogId());
		calls.put(s.getDialogId(), new TestCallState(s.getDialogId(), TestCallState.PROGRESS));
		
		// force crash
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				System.exit(0);
			}
		}, 500);
	}

	@Override
	public void progress(Session chan) {
		logger.info("Progress : " + chan.getDialogId());
		TestCallState cs = (TestCallState)calls.get(chan.getDialogId());
		if (cs != null) {
			cs.setState(TestCallState.PROGRESS);
			calls.put(chan.getDialogId(), cs);
		}
	}

	@Override
	public void connected(Session chan) {
		logger.info("Connected : " + chan.getDialogId());
		TestCallState cs = (TestCallState)calls.get(chan.getDialogId());
		if (cs != null) {
			cs.setState(TestCallState.CONNECT);
			calls.put(chan.getDialogId(), cs);
		}
	}

	@Override
	public void disconnected(Session chan) {
		logger.info("Disconnected : " + chan.getDialogId());
		TestCallState cs = (TestCallState)calls.get(chan.getDialogId());
		if (cs != null) {
			cs.setState(TestCallState.DISCONNECT);
			calls.put(chan.getDialogId(), cs);
		}
	}

	@Override
	public void message(Session chan, String text) {
		logger.info("Message : " + chan.getDialogId());
		
	}

	@Override
	public void dtmf(Session chan) {
		logger.info("DTMF : " + chan.getDialogId());
		
	}

	@Override
	public void terminated(Session chan) {
		logger.info("Terminated : " + chan.getDialogId());
		chan.registerListener(null);
		calls.remove(chan.getDialogId());
	}
	
	@Override
	public void updateMedia(Session chan) {
		final String dialogId = chan.getDialogId();
		
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				ClusterImpl.getInstance().queueAction(new UpdateAck(dialogId));
				
			}
		}, 2000);
		
	}

	@Override
	public void handover(Session chan) {
		logger.info("Channel handover : " + chan.getDialogId());
		chan.registerListener(this);
		TestCallState cs = (TestCallState)calls.get(chan.getDialogId());
		if (cs != null) {
			if (cs.getState().equals(TestCallState.PROGRESS)) {
				final String dialogId = chan.getDialogId();
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						cluster.doAnswer(dialogId, null);
					}
				}, 2000);
			} else {
				logger.info("Call not in Progress state : " + chan.getDialogId());
				
			}
		}
	}

	@Override
	public int registration(String user, String uri) {
		logger.info("Registration: " + user + " -> " + uri);
		return 60;
	}

	@Override
	public void report() {
		for(Object cs : calls.values()) {
			System.out.println(((TestCallState)cs).getId().substring(0, 15) + " : " + ((TestCallState)cs).getState());
		}
	}
}
