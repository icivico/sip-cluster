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
