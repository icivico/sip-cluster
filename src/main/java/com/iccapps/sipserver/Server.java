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

package com.iccapps.sipserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.iccapps.sipserver.api.Cluster;
import com.iccapps.sipserver.cluster.ClusterException;
import com.iccapps.sipserver.cluster.hz.ClusterImpl;
import com.iccapps.sipserver.exception.StackNotInitialized;

public class Server {

	private static Logger logger = Logger.getLogger(Server.class);
	
	public Server() {
	}
	
	public void init() throws FileNotFoundException, IOException, StackNotInitialized, ClusterException {
		ClusterImpl.getInstance().start();
	}
	
	public void exit() {
		ClusterImpl.getInstance().stop();
	}
	
    public static void main( String[] args ) throws FileNotFoundException, IOException, StackNotInitialized, ClusterException {
    	boolean daemon = false;
		
		if (args.length > 0 && args[0].equals("-d")) {
			daemon = true;
			System.out.close();
			System.err.close();
		}
		
		Server app = new Server();
		app.init();
		
		try {
			if (!daemon) {
				int c = 0;
				while ( (c = System.in.read()) != 'q') {
					if (c == 'd') ClusterImpl.getInstance().reportNodes();
					else if (c == 's') ClusterImpl.getInstance().getService().report();
					
					Thread.sleep(200);
				}
			} else {
				while (true) {
					Thread.sleep(200);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		app.exit();
		System.exit(0);
    }
}
