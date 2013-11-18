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

package com.iccapps.sipserver.service.jsr309;

import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.join.JoinEvent;
import javax.media.mscontrol.join.JoinEventListener;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.mediagroup.signals.SignalDetectorEvent;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;

import org.apache.log4j.Logger;

import com.iccapps.sipserver.api.Controller;
import com.iccapps.sipserver.api.Session;

public class OnlyCollectDTMF implements Controller {

	private static Logger logger = Logger.getLogger(OnlyCollectDTMF.class);

	private String dialogId;
	private String remoteSDP;
	private String localSDP;
	private ServiceWithMedia service;
	private MediaSession mediaSession;
	private NetworkConnection conn;
	private NetworkListener ncListener;
	private DTMFListener dtmlListener;

	private class NetworkListener implements
			MediaEventListener<SdpPortManagerEvent>,
			JoinEventListener {

		@Override
		public void onEvent(SdpPortManagerEvent ev) {

			if (ev.isSuccessful()) {
				byte[] msSDP = ev.getMediaServerSdp();
				localSDP = new String(msSDP);

				logger.info("NetworkConnection created, answer call " + dialogId);
				service.cluster.doAnswer(dialogId, localSDP);

			} else {
				logger.info("No SDP, reject call " + dialogId);
				service.cluster.doReject(dialogId, 500);
			}
		}
	
		@Override
		public void onEvent(JoinEvent event) {
			MediaGroup mg = (MediaGroup) event.getThisJoinable();
			if (event.isSuccessful()) {
				if (JoinEvent.JOINED == event.getEventType()) {
					// NC Joined to MG

					if (logger.isDebugEnabled()) {
						logger.debug("NC joined to MG. Start DTMF detector");
					}
					try {
						SignalDetector sg = mg.getSignalDetector();
						dtmlListener = new DTMFListener();
						sg.addListener(dtmlListener);
						Parameters params = mg.createParameters();
						params.put(SignalDetector.INITIAL_TIMEOUT, 5000);
						params.put(SignalDetector.INTER_SIG_TIMEOUT, 5000);
						params.put(SignalDetector.MAX_DURATION, 15000);
						sg.receiveSignals(5, null, null, params);

					} catch (Exception e) {
						logger.error(e);
					}
				} else if (JoinEvent.UNJOINED == event.getEventType()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Un-Joined MG and NC");
					}
				}

			} else {
				logger.error("Joining of MG and NC failed");
			}

		}
	}

	private class DTMFListener implements
			MediaEventListener<SignalDetectorEvent> {

		@Override
		public void onEvent(SignalDetectorEvent event) {

			if (event.isSuccessful()
					&& (SignalDetectorEvent.RECEIVE_SIGNALS_COMPLETED == event
							.getEventType())) {
				logger.error("DTMF detection success " + event.getSignalString());

			} else {
				logger.error("DTMF detection failed " + event.getSignalString());
			}

			// disconnect call
			service.cluster.doHangup(dialogId);
		}
	}

	public OnlyCollectDTMF(ServiceWithMedia s, String d) {
		dialogId = d;
		service = s;
	}

	@Override
	public void incoming(Session chan) {
		logger.info("Incoming " + chan.getDialogId());

		try {
			mediaSession = service.msControlFactory.createMediaSession();
			conn = mediaSession
					.createNetworkConnection(NetworkConnection.BASIC);
			SdpPortManager sdpManag = conn.getSdpPortManager();

			ncListener = new NetworkListener();
			sdpManag.addListener(ncListener);
			remoteSDP = chan.getRemoteSDP();
			sdpManag.processSdpOffer(remoteSDP.getBytes());

		} catch (MsControlException e) {
			e.printStackTrace();
			service.cluster.doReject(dialogId, 500);
		}
	}

	@Override
	public void updateMedia(Session chan) {
		// TODO Auto-generated method stub

	}

	@Override
	public void progress(Session chan) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connected(Session chan) {
		logger.info("Connected " + chan.getDialogId());

		try {
			MediaGroup mg = mediaSession
					.createMediaGroup(MediaGroup.SIGNALDETECTOR);
			mg.addListener(ncListener);
			mg.join(Direction.RECV, conn);

		} catch (MsControlException e) {
			logger.error(e);
			service.cluster.doHangup(dialogId);
		}
	}

	@Override
	public void disconnected(Session chan) {
		logger.info("Disconnected " + chan.getDialogId());
		ncListener = null;
		if (conn != null)
			conn.release();
		conn = null;
		if (mediaSession != null)
			mediaSession.release();
		mediaSession = null;

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
		ncListener = null;
		if (conn != null)
			conn.release();
		conn = null;
		if (mediaSession != null)
			mediaSession.release();
		mediaSession = null;
	}

}
