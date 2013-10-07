package com.iccapps.sipserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.iccapps.sipserver.api.Cluster;
import com.iccapps.sipserver.cluster.hz.ClusterImpl;
import com.iccapps.sipserver.exception.StackNotInitialized;

public class Server {

	private static Logger logger = Logger.getLogger(Server.class);
	private static Properties config;
	
	private Endpoint ep;
	
	static {
		config = new Properties();
		try {
			config.load(new FileInputStream(new File("server.properties")));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		PropertyConfigurator.configure("log4j.properties");
	}
	
	public static Properties getConfig() {
		return config;
	}
	
	public Server() {
		Cluster c = ClusterImpl.getInstance();
		ServiceManager.getInstance().initialize(c);
		ep = Endpoint.getInstance();
		
	}
	
	public void init() throws FileNotFoundException, IOException, StackNotInitialized {
		ep.start();
	}
	
	public void exit() {
		ep.stop();
	}
	
    public static void main( String[] args ) throws FileNotFoundException, IOException, StackNotInitialized {
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
					else if (c == 's') ServiceManager.getInstance().report();
					
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
