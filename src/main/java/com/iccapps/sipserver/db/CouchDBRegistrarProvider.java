package com.iccapps.sipserver.db;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.lightcouch.CouchDbClient;

import com.iccapps.sipserver.sip.registrar.Binding;
import com.iccapps.sipserver.sip.registrar.SipUser;

public class CouchDBRegistrarProvider implements RegistrarDbProvider {
	
	private class Configuration {
		private List<String> domains;
		private int registrationTimeout;
	}
	
	private CouchDbClient dbClient;
	
	@Override
	public void configure(Properties config) {
		dbClient =  new CouchDbClient(
							config.getProperty("couchdb.name", "registrar"),
							false,
							"http",
							config.getProperty("couchdb.host", "localhost"),
							Integer.parseInt(config.getProperty("couchdb.port", "5984")),
							config.getProperty("couchdb.username"),
							config.getProperty("couchdb.password"));
	}

	@Override
	public boolean isDomainSupported(String domain) {
		Configuration c = dbClient.find(Configuration.class, "configuration");
		if (c != null && c.domains.contains(domain)) return true;
		return false;
	}

	@Override
	public SipUser findSipUser(String user, String domain) {
		String id = dbClient.view("users/by_username")
			.key(Arrays.asList(user, domain))
			.queryForString();
		if (id != null)
			return dbClient.find(SipUser.class, id);
		else
			return null;
	}

	@Override
	public List<Binding> getBindings(String aor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveBinding(Binding binding) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBinding(Binding binding) {
		// TODO Auto-generated method stub

	}
	
	public static void main( String[] args ) {
		Properties p = new Properties();
		p.setProperty("couchdb.name", "registrar");
		p.setProperty("couchdb.host", "localhost");
		CouchDBRegistrarProvider db = new CouchDBRegistrarProvider();
		db.configure(p);
		System.out.println("domain icc-apps.es: " + db.isDomainSupported("icc-apps.es"));
		System.out.println("domain google.com: " + db.isDomainSupported("google.com"));
		SipUser u = db.findSipUser("icivico", "icc-apps.es");
		System.out.println("User : " + u.getUsername() + "@" + u.getDomain() + " <" + u.getDisplay() + ">");
	}
}
