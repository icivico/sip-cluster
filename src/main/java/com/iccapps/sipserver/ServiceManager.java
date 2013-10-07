package com.iccapps.sipserver;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import com.iccapps.sipserver.api.Service;

public abstract class ServiceManager {

	private static Service instance;
	
	public static synchronized Service getInstance() {
		if (instance == null) createInstance();
		return instance;
	}
	
	private static void createInstance() {
		// get configuration
		String name = Server.getConfig().getProperty(Constants.SERVICE_CLASS_NAME);
		try {
			instance = (Service) Class.forName(name).getConstructor(Properties.class).newInstance(Server.getConfig());
		
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
