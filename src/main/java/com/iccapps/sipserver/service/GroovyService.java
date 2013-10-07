package com.iccapps.sipserver.service;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.codehaus.groovy.control.CompilationFailedException;

import groovy.lang.GroovyClassLoader;

import com.iccapps.sipserver.api.Cluster;
import com.iccapps.sipserver.api.Service;
import com.iccapps.sipserver.api.Session;

public class GroovyService implements Service {
	
	private Service service;

	public GroovyService(Properties p) {
		
	}
	
	@Override
	public void initialize(Cluster c) {
		ClassLoader parent = getClass().getClassLoader();
		GroovyClassLoader loader = new GroovyClassLoader(parent);
		try {
			Class groovyClass = loader.parseClass(new File("service.groovy"));
			Object o = groovyClass.newInstance();
			service = (Service)o;
			service.initialize(c);
			
		} catch (CompilationFailedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void start(Session s) {
		service.start(s);

	}

	@Override
	public void stop(Session s) {
		service.stop(s);

	}

	@Override
	public void handover(Session s) {
		service.handover(s);
	}

	@Override
	public int registration(String user, String uri) {
		return service.registration(user, uri);
	}

	@Override
	public void report() {
		service.report();

	}

}
