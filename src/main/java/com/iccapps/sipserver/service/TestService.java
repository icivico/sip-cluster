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

package com.iccapps.sipserver.service;

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

public class TestService implements Service, Controller {
	
	private static Logger logger = Logger.getLogger(TestService.class);
	
	private Timer timer = new Timer();
	private Cluster cluster;
	
	public TestService() { }
	
	@Override
	public void configure(Properties config) { }
	
	@Override
	public void initialize(Cluster c) {
		cluster = c;
	}
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
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
		
		final String dialogId = s.getDialogId();
		
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				cluster.doAnswer(dialogId, null);
				
			}
		}, 2000);

		
	}

	@Override
	public void progress(Session chan) {
		logger.info("Progress : " + chan.getDialogId());
		
	}

	@Override
	public void connected(Session chan) {
		logger.info("Connected : " + chan.getDialogId());
		
	}

	@Override
	public void disconnected(Session chan) {
		logger.info("Disconnected : " + chan.getDialogId());
		
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
	}

	@Override
	public int registration(String user, String uri) {
		logger.info("Registration: " + user + " -> " + uri);
		return 60;
	}

	@Override
	public void report() {
		// TODO Auto-generated method stub
		
	}
}
