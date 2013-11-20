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

package com.iccapps.sipserver.cluster.hz;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;
import org.mobicents.ha.javax.sip.ClusteredSipStack;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.Member;
import com.iccapps.sipserver.action.Action;
import com.iccapps.sipserver.action.Answer;
import com.iccapps.sipserver.action.Hangup;
import com.iccapps.sipserver.action.OutboundCall;
import com.iccapps.sipserver.action.Reject;
import com.iccapps.sipserver.api.Cluster;
import com.iccapps.sipserver.api.Constants;
import com.iccapps.sipserver.api.Service;
import com.iccapps.sipserver.api.Session;
import com.iccapps.sipserver.cluster.ClusterException;
import com.iccapps.sipserver.cluster.NodeData;
import com.iccapps.sipserver.exception.StackNotInitialized;
import com.iccapps.sipserver.sip.Endpoint;
import com.iccapps.sipserver.sip.RegistrarImpl;
import com.iccapps.sipserver.sip.SessionImpl;
import com.iccapps.sipserver.sip.SessionState;

public class ClusterImpl implements Cluster {
	
	private static Logger logger = Logger.getLogger(ClusterImpl.class);
	
	private static ClusterImpl instance;
	private static Properties config;
	// ONLY for debug
	public static boolean optionsOnlyToBalancer = true;
	
	protected Endpoint sipEndpoint;
	public Endpoint getSipEndpoint() {
		return sipEndpoint;
	}
	
	protected RegistrarImpl registrar;

	protected Service service;
	public Service getService() {
		return service;
	}

	protected HazelcastInstance hz;
	private String uuid;
	private NodeData node;
	
	/**
	 * Key: node uuid
	 * Value: channels handled by node  
	 */
	private IMap<String, NodeData> nodes;
	private IQueue<Action> actionq;
	private IQueue<SessionState> orphans;
	
	private Thread thActionDispatcher;
	private Thread thChannelHandover;
	private boolean finished = false;
	protected Lock failoverLock;
	protected Lock actionsLock;
	
	static {
		config = new Properties();
		try {
			config.load(new FileInputStream(new File("cluster.properties")));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static ClusterImpl getInstance() {
		if (instance == null)
			instance = new ClusterImpl();
		
		return instance;
	}
	
	private ClusterImpl() { }
	
	private Endpoint createSipEndpoint() {
		return new Endpoint(config, this);
	}
	
	private Service createServiceInstance() throws ClusterException {
		Service srv = null;
		String name = config.getProperty(Constants.SERVICE_CLASS_NAME);
		try {
			srv = (Service) Class.forName(name).newInstance();
			srv.configure(config);
		
		} catch (Exception e) {
			e.printStackTrace();
			throw new ClusterException("Could not instantiate Service");
		}
		
		return srv;
	}
	
	public void start() throws ClusterException {
		sipEndpoint = createSipEndpoint(); 
		service = createServiceInstance();
		
		startSipEndpoint();
		startCluster();	
		startRegistrar();
		initializeService();
	}
	
	private void startSipEndpoint() throws ClusterException {
		try {
			sipEndpoint.start();
			
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			throw new ClusterException("Endpoint not initialized");
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new ClusterException("Endpoint not initialized");
		} catch (StackNotInitialized e1) {
			e1.printStackTrace();
			throw new ClusterException("Endpoint not initialized");
		}
	}
	
	private void startCluster() throws ClusterException {
		// start cluster
		hz = Hazelcast.getHazelcastInstanceByName("jain-sip-ha");
		if (hz == null) 
			throw new ClusterException("No hazelcast cache");
        uuid = hz.getCluster().getLocalMember().getUuid();
        node = new NodeData();
        node.setName(uuid);
        
        logger.debug("My cluster uuid: " + uuid);
        
        nodes = hz.getMap("cluster.nodes");
        nodes.put(uuid, node);
        actionq = hz.getQueue("cluster.actions");
        orphans = hz.getQueue("cluster.orphans");
        failoverLock = hz.getLock("cluster.lock.failover");
        actionsLock = hz.getLock("cluster.lock.actions");
        
        thActionDispatcher = new Thread(new Runnable() {
			@Override
			public void run() {
				int tsleep;
				Action action;
				while(!finished) {
					action = null;
					tsleep = 0;
					// acquire lock
					actionsLock.lock();
					try {
						action = actionq.peek();
						if (action != null) {
							// check dialogId
							String dialogId = action.getDialogId();
							if (dialogId == null) {
								actionq.remove(action);
								// no owner, we process action outside the lock
								
							} else if (node.getChannels().containsKey(dialogId)) {
								actionq.remove(action);
								// we are owner, process action outside the lock
								
							} else {
								// do nothing and let another node to consume
								tsleep = 200;
							}
								
						} else {
							// no commands, sleep for a while
							tsleep = 100;
						}
						
					} finally {
						actionsLock.unlock();
					}
					
					// dispatch action if we got one
					if (action != null) {
						sipEndpoint.dispatch(action);
						continue;
					}
					
					// sleep if requested
					if (tsleep > 0) {
						try {
							Thread.sleep(tsleep);
							
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}
					}
				}
			}
		});
        thActionDispatcher.start();
        
        thChannelHandover = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while(!finished) {
					try {
						SessionState c = orphans.poll();
						if (c != null) {
							logger.info("Handover of channel: " + c.getDialogId());
							recover(c);
							
							Thread.sleep(500);
							
						} else {
							Thread.sleep(1000);
						}
						
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
				}
			}
		});
        thChannelHandover.start();
	}
	
	private void startRegistrar() {
		try {
			boolean enabled = Boolean.parseBoolean(config.getProperty(Constants.REGISTRAR_ENABLED, "false"));
			if (enabled) 
				registrar = new RegistrarImpl(sipEndpoint, this, config);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initializeService() {
		service.initialize(this);
	}
	
	public void stop() {
		stopCluster();
		stopSipEndpoint();
		destroyService();
	}

	private void stopCluster() {
		finished = true;
		try {
			if (thChannelHandover == null) {
				thChannelHandover.join(500);
				thChannelHandover = null;
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			if (thActionDispatcher == null) {
				thActionDispatcher.join(500);
				thActionDispatcher = null;
			}
		
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void stopSipEndpoint() {
		if (sipEndpoint != null) {
			sipEndpoint.stop();
			sipEndpoint = null;
		}
	}
	
	private void destroyService() {
		if (service == null) {
			service.destroy();
			service = null;
		}
	}
	
	public void finalize() {
		if (!finished)
			stop();
	}
	
	public void register(SessionImpl chan) {
		// register in distributed cache
		node.getChannels().put(chan.getDialogId(), chan.getData());
		nodes.put(uuid, node);
	}
	
	public void unregister(SessionImpl chan) {
		node.getChannels().remove(chan.getDialogId());
		nodes.put(uuid, node);
	}
	
	public void update(SessionImpl chan) {
		// update channel
		node.getChannels().put(chan.getDialogId(), chan.getData());
		nodes.put(uuid, node);
	}
	
	public void reportNodes() {
		System.out.println("============ Coordinator state =============");
		for(NodeData n : nodes.values()) {
			if (n.getName().equals(uuid))
				System.out.println("------ THIS NODE ------");
			else
				System.out.println("------ Remote node ------");
			System.out.println("  name: " + n.getName());
			for (SessionState c : n.getChannels().values()) {
				System.out.println("  >>> Channel <<<");
				System.out.println("  dialog: " + c.getDialogId().substring(0,15) + "...");
				System.out.println("  state: " + c.getState());
			}
		}
		System.out.println("=========================================");
		
		System.out.println("=========================================");
		Map<String, Object> st = hz.getMap("cache.serverTX");
		for (String s : st.keySet()) {
			boolean exists = ((ClusteredSipStack)sipEndpoint.getSipStack()).findTransaction(s, false) != null;
			System.out.println("ServerTransaction: " + s + ", in stack: " + exists);
		}
		System.out.println("=========================================");
	}
	
	public void queueAction(Action a) {
		logger.debug("Queue action " + a.toString() + " for channel " + a.getDialogId());
		actionq.offer(a);
	}
	
	public void handleServerFailure(Member member) {
		failoverLock.lock();
		try {
			String failedNode = member.getUuid();
			NodeData n = nodes.get(failedNode);
			if (n != null) {
				logger.info("Detected failed node " + n.getName());
				for (SessionState chan : n.getChannels().values()) {
					// queue channel for some node catching
					logger.info("Channel failed, offer for recovering " + chan.getDialogId());
					orphans.offer(chan);
				}
				// remove node
				logger.info("Remove failed node " + n.getName());
				nodes.remove(failedNode);
				
			} else {
				logger.warn("Node info not found");
			}
		} finally {
			failoverLock.unlock();
		}
	}
	
	private boolean recover(SessionState chan) {
		
		logger.info("Recovering session " + chan.getDialogId());
		
		boolean res = false;
		
		try {
			SessionImpl c = new SessionImpl(chan);
			sipEndpoint.recover(c);
			service.handover(c);
			node.getChannels().put(c.getDialogId(), chan);
			nodes.put(uuid, node);
			c.recovered();
			res = true;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return res;
	}

	@Override
	public Map<String, Object> createDistributedMap(String name) {
		return hz.getMap(name);
	}
	
	@Override
	public List<Object> createDistributedList(String name) {
		return hz.getList(name);
	}
	
	@Override
	public Lock createDistributedLock(String name) {
		return hz.getLock(name);
	}

	@Override
	public void originate(String originationUri, String destinationUri, String sdp, String reference) {
		queueAction(new OutboundCall(null, originationUri, destinationUri, sdp, reference));
	}

	@Override
	public Session getSession(String sessionId) {
		return sipEndpoint.getSession(sessionId);
		
	}
	
	@Override
	public void doAnswer(String dialogId, String sdp) {
		queueAction(new Answer(dialogId, sdp));
	}
	
	@Override
	public void doHangup(String dialogId) {
		queueAction(new Hangup(dialogId));
	}
	
	@Override
	public void doReject(String dialogId, int code) {
		queueAction(new Reject(dialogId, code));
	}
}
