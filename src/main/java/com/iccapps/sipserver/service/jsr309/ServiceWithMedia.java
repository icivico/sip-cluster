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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.media.mscontrol.MsControlFactory;

import org.apache.log4j.Logger;
import org.mobicents.javax.media.mscontrol.spi.DriverImpl;

import com.iccapps.sipserver.api.Cluster;
import com.iccapps.sipserver.api.Controller;
import com.iccapps.sipserver.api.Service;
import com.iccapps.sipserver.api.Session;

public class ServiceWithMedia implements Service {
	
    // Property key for the Unique MGCP stack name for this application 
    private static final String MGCP_STACK_NAME = "mgcp.stack.name"; 
    // Property key for the IP address where CA MGCP Stack (SIP Servlet 
    // Container) is bound 
    private static final String MGCP_STACK_IP = "mgcp.server.address"; 
    // Property key for the port where CA MGCP Stack is bound 
    private static final String MGCP_STACK_PORT = "mgcp.local.port"; 
    // Property key for the IP address where MGW MGCP Stack (MMS) is bound 
    private static final String MGCP_PEER_IP = "mgcp.bind.address"; 
    // Property key for the port where MGW MGCP Stack is bound 
    private static final String MGCP_PEER_PORT = "mgcp.server.port"; 
	
    private static Logger logger = Logger.getLogger(ServiceWithMedia.class);
	protected Cluster cluster;
	protected MsControlFactory msControlFactory;
	private Map<String, Controller> calls = new HashMap<String, Controller>();
	
	public ServiceWithMedia(Properties config) {}
	
	@Override
	public void initialize(Cluster c) {
		cluster = c;
		//HazelcastInstance hz = Hazelcast.getHazelcastInstanceByName("jain-sip-ha");
		//bridges = hz.getList("mediatest.bridges");
		Properties properties = new Properties();
        properties.setProperty(MGCP_STACK_NAME, "SipCluster1");
        properties.setProperty(MGCP_PEER_IP, "127.0.0.1");
        properties.setProperty(MGCP_PEER_PORT, "2427");

        properties.setProperty(MGCP_STACK_IP, "127.0.0.1");
        properties.setProperty(MGCP_STACK_PORT, "2727");

        try {
            // create the Media Session Factory
        	DriverImpl drv = new DriverImpl();
        	msControlFactory = drv.getFactory(properties);
        	logger.info("started MGCP Stack on 127.0.0.1 and port 2727");
            
        } catch (Exception e) {
            logger.error("couldn't start the underlying MGCP Stack", e);
        }
	}

	@Override
	public void destroy() {
		
	}

	@Override
	public void start(Session s) {
		Controller app = new OnlyCollectDTMF(this, s.getDialogId());
		calls.put(s.getDialogId(), app);
		s.registerListener(app);
	}

	@Override
	public void stop(Session s) {
		calls.remove(s.getDialogId());
		s.registerListener(null);

	}

	@Override
	public void handover(Session s) {
		// TODO Auto-generated method stub

	}

	@Override
	public int registration(String user, String uri) {
		return 60;
	}

	@Override
	public void report() {
		// TODO Auto-generated method stub

	}

}
