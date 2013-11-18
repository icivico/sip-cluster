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

package com.iccapps.sipserver.sip;

import java.lang.reflect.InvocationTargetException;
import java.util.TimerTask;

import javax.sip.Transaction;

import org.apache.log4j.Logger;

import com.iccapps.sipserver.action.Action;
import com.iccapps.sipserver.action.Answer;
import com.iccapps.sipserver.action.Hangup;
import com.iccapps.sipserver.action.Reject;
import com.iccapps.sipserver.action.UpdateAck;
import com.iccapps.sipserver.api.Controller;
import com.iccapps.sipserver.api.Session;
import com.iccapps.sipserver.cluster.hz.ClusterImpl;

public class SessionImpl implements Session {
	
	private static Logger logger = Logger.getLogger(SessionImpl.class);

	private SessionState data;
	
	private Controller controller;
	private State state;
	private Endpoint ep;
	
	private Transaction transaction = null;
	private TimerTask keepaliveTask;
	
	public SessionImpl() {
		data = new SessionState();
		state = new Initial(this);
		data.setState(state.getClass().getName());
		ep = ClusterImpl.getInstance().getSipEndpoint();
	}
	
	public SessionImpl(SessionState s) throws IllegalArgumentException, SecurityException, 
				InstantiationException, IllegalAccessException, InvocationTargetException, 
				NoSuchMethodException, ClassNotFoundException {
		data = s;
		state = (State) Class.forName(data.getState()).getConstructor(SessionImpl.class).newInstance(this);
		ep = ClusterImpl.getInstance().getSipEndpoint();
	}
	
	public void registerListener(Controller c) {
		controller = c;
	}
	
	public void replicate() {
		ClusterImpl.getInstance().update(this);
	}
	
	public void init() {
		
	}
	
	public void end() {
		if (keepaliveTask != null) {
			keepaliveTask.cancel();
			keepaliveTask = null;
		}
		
		transaction = null;
		controller = null;
		state = null;
		data = null;
	}
	
	public void dispatch(Action a) {
		logger.debug("Execute action " + a.toString() + " on channel " + SessionImpl.this.data.getDialogId());
		if (a instanceof Answer) {
			SessionImpl.this.data.setLocalSDP(((Answer) a).getSdp());
			SessionImpl.this.answer();
			
		} else if(a instanceof UpdateAck) {
			SessionImpl.this.updateAck();
			
		} else if(a instanceof Reject) {
			SessionImpl.this.hangup(((Reject) a).getCode());
			
		} else if(a instanceof Hangup) {
			SessionImpl.this.hangup();
		}
	}
	
	//*********** actions defined ***************//
	public void answer() {
		if (state != null) state.answer();
	}
	
	public void place(String destination) {
		
	}
	
	public void hangup() {
		if (state != null) state.hangup();
	}
	
	public void hangup(int reason) {
		if (state != null) state.reject(reason);
	}
	
	public void recovered() {
		if (state != null) {
			state.keepalive();
		}
		// activate options
		keepaliveTask = new TimerTask() {
			@Override
			public void run() {
				if (state != null) state.keepalive();
				
			}
		};
		ep.getTimer().schedule(keepaliveTask, 5000, 5000);
	}
	
	public void updateAck() {
		if (state != null) state.update();
	}
	
	//************ Events defined ******************//
	public void fireIncoming() {
		if (controller != null)
			controller.incoming(this);
	}
	
	public void fireConnected() {
		// activate options
		keepaliveTask = new TimerTask() {
			@Override
			public void run() {
				if (state != null)
					state.keepalive();
				
			}
		};
		ep.getTimer().schedule(keepaliveTask, 5000, 5000);
		
		if (controller != null)
			controller.connected(this);
	}
	
	public void fireDisconnected() {
		if (keepaliveTask != null) {
			keepaliveTask.cancel();
			keepaliveTask = null;
		}
		
		if (controller != null)
			controller.disconnected(this);
	}
	
	public void fireProceeding() {
		if (controller != null)
			controller.progress(this);
	}
	
	public void fireTerminated() {
		if (keepaliveTask != null) {
			keepaliveTask.cancel();
			keepaliveTask = null;
		}
		
		if (controller != null)
			controller.terminated(this);
	}
	
	public void fireMessage(String message) {
		if (controller != null)
			controller.message(this, message);
	}
	
	public void fireDTMF() {
		if (controller != null)
			controller.dtmf(this);
	}
	
	public void fireUpdateMedia() {
		if (controller != null)
			controller.updateMedia(this);
	}
	
	// --------------------------------------------
	public String getDialogId() {
		return data.getDialogId();
	}
	
	public void setDialogId(String dialogId) {
		data.setDialogId(dialogId);
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
		this.data.setState(state.getClass().getName());
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public void setTransaction(Transaction transaction) {
		this.transaction = transaction;
	}

	public String getLocalSDP() {
		return data.getLocalSDP();
	}

	public void setLocalSDP(String localSDP) {
		data.setLocalSDP(localSDP);
	}

	public String getRemoteSDP() {
		return data.getRemoteSDP();
	}

	public void setRemoteSDP(String remoteSDP) {
		data.setRemoteSDP(remoteSDP);
	}

	public boolean isCanceled() {
		return data.isCanceled();
	}

	public void setCanceled(boolean canceled) {
		data.setCanceled(canceled);
	}

	public String getOriginationURI() {
		return data.getOriginURI();
	}

	public void setOriginURI(String originURI) {
		data.setOriginURI(originURI);
	}

	public String getDestinationURI() {
		return data.getDestinationURI();
	}

	public void setDestinationURI(String destinationURI) {
		data.setDestinationURI(destinationURI);
	}
	
	public String getReference() {
		return data.getReference();
	}

	public SessionState getData() {
		return data;
	}
}
