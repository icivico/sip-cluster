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

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.Member;
import com.iccapps.sipserver.Endpoint;
import com.iccapps.sipserver.ServiceManager;
import com.iccapps.sipserver.action.Action;
import com.iccapps.sipserver.action.Answer;
import com.iccapps.sipserver.action.Hangup;
import com.iccapps.sipserver.action.OutboundCall;
import com.iccapps.sipserver.action.Reject;
import com.iccapps.sipserver.api.Cluster;
import com.iccapps.sipserver.api.Session;
import com.iccapps.sipserver.cluster.ClusterException;
import com.iccapps.sipserver.cluster.NodeData;
import com.iccapps.sipserver.session.SessionImpl;
import com.iccapps.sipserver.session.SessionState;

public class ClusterImpl implements Cluster {
	
	private static Logger logger = Logger.getLogger(ClusterImpl.class);
	
	private static ClusterImpl instance;
	
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
	
	
	public static ClusterImpl getInstance() {
		if (instance == null)
			instance = new ClusterImpl();
		
		return instance;
	}
	
	private ClusterImpl() {
	}
	
	public void start() throws ClusterException {
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
				while(!finished) {
					actionsLock.lock();
					try {
						Action a = actionq.peek();
						if (a != null) {
							// check dialogId
							String dialogId = a.getDialogId();
							if (dialogId == null) {
								actionq.remove(a);
								Endpoint.getInstance().dispatch(a);
								
							} else if (node.getChannels().containsKey(dialogId)) {
								actionq.remove(a);
								Endpoint.getInstance().dispatch(a);
								
							} else {
								// do nothing, we let other node to consume
								Thread.sleep(200);
							}
								
						} else {
							// do nothing, we let other node to consume
							Thread.sleep(100);
						}
						
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
						
					} finally {
						actionsLock.unlock();
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
	
	public void stop() {
		finished = true;
		try {
			thChannelHandover.join(500);
			thChannelHandover = null;
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			thActionDispatcher.join(500);
			thActionDispatcher = null;
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void finalize() {
		finished = true;
		try {
			if (thActionDispatcher != null)
				thActionDispatcher.join(500);
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			if (thChannelHandover != null)
				thChannelHandover.join(500);
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
		
		int i =0;
		for (SessionState s : node.getChannels().values()) {
		    System.out.println("Chan entry " + i +": " + s.getDialogId());
		    i++;
		}
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
				logger.info("Remove failed node " + node.getName());
				nodes.remove(failedNode);
				
			} else {
				logger.warn("Node info not found");
			}
		} finally {
			failoverLock.unlock();
		}
	}
	
	private boolean recover(SessionState chan) {
		
		boolean res = false;
		
		try {
			SessionImpl c = new SessionImpl(chan);
			Endpoint.getInstance().recover(c);
			ServiceManager.getInstance().handover(c);
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
		return Endpoint.getInstance().getSession(sessionId);
		
	}
	
	@Override
	public void doAnswer(String dialogId, String sdp) {
		ClusterImpl.getInstance().queueAction(new Answer(dialogId, sdp));
	}
	
	@Override
	public void doHangup(String dialogId) {
		ClusterImpl.getInstance().queueAction(new Hangup(dialogId));
	}
	
	@Override
	public void doReject(String dialogId, int code) {
		ClusterImpl.getInstance().queueAction(new Reject(dialogId, code));
	}
}
