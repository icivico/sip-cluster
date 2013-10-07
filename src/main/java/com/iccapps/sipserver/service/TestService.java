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
	
	public TestService(Properties config) {
	}
	
	@Override
	public void initialize(Cluster c) {
		cluster = c;
	}
	
	@Override
	public void start(Session chan) {
		chan.registerListener(this);

	}

	@Override
	public void stop(Session chan) {
		
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
