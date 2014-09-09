package com.iccapps.sipserver.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.gson.Gson;
import com.iccapps.sipserver.sip.registrar.Binding;
import com.iccapps.sipserver.sip.registrar.SipUser;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class OrientDbRegistrarProvider implements RegistrarDbProvider {
	
	private static final String ORIENTDB_URL = "orientdb.url";
	private static final String ORIENTDB_USER = "orientdb.username";
	private static final String ORIENTDB_PASS = "orientdb.password";
	
	private String url;
	private String dbuser;
	private String dbpass;

	public OrientDbRegistrarProvider() {
	}
	
	/* (non-Javadoc)
	 * @see com.iccapps.sipserver.db.RegistrarDbProvider#configure(java.util.Properties)
	 */
	@Override
	public void configure(Properties config) {
		url = config.getProperty(ORIENTDB_URL, "remote:localhost/registrar");
		dbuser = config.getProperty(ORIENTDB_USER, "guest");
		dbpass = config.getProperty(ORIENTDB_PASS, "guest");
	}

	/* (non-Javadoc)
	 * @see com.iccapps.sipserver.db.RegistrarDbProvider#isDomainSupported(java.lang.String)
	 */
	@Override
	public boolean isDomainSupported(String domain) {
		ODatabaseDocumentTx db = ODatabaseDocumentPool.global().acquire(url, dbuser, dbpass);
		try {
			List<ODocument> result = db.query(
				    new OSQLSynchQuery<ODocument>("select * from domain where name = '" + domain + "'"));
			if (result != null && result.size() > 0) {
				return true;
			}

		} finally {
			db.close();
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.iccapps.sipserver.db.RegistrarDbProvider#findSipUser(java.lang.String, java.lang.String)
	 */
	@Override
	public SipUser findSipUser(String user, String domain) {
		ODatabaseDocumentTx db = ODatabaseDocumentPool.global().acquire(url, dbuser, dbpass);
		try {
			List<ODocument> result = db.query(
				    new OSQLSynchQuery<ODocument>("select * from sipuser where username = '" + user + 
				    									"' and domain = '" + domain + "'"));
			if (result != null && result.size() == 1) {
				return new Gson().fromJson(result.get(0).toJSON(), SipUser.class);
			}

		} finally {
			db.close();
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.iccapps.sipserver.db.RegistrarDbProvider#getBindings(java.lang.String)
	 */
	@Override
	public List<Binding> getBindings(String aor) {
		List<Binding> bindings = null;
		ODatabaseDocumentTx db = ODatabaseDocumentPool.global().acquire(url, dbuser, dbpass);
		try {
			List<ODocument> result = db.query(
				    new OSQLSynchQuery<ODocument>("select * from binding where aor = '" + aor + "'"));
			if (result != null) {
				bindings = new ArrayList<Binding>();
				Gson g = new Gson();
				for (ODocument doc : result) {
					bindings.add(g.fromJson(doc.toJSON(), Binding.class));
				}
			}

		} finally {
			db.close();
		}
		
		return bindings;
	}
	
	/* (non-Javadoc)
	 * @see com.iccapps.sipserver.db.RegistrarDbProvider#saveBinding(com.iccapps.sipserver.sip.registrar.Binding)
	 */
	@Override
	public void saveBinding(Binding binding) {
		ODatabaseDocumentTx db = ODatabaseDocumentPool.global().acquire(url, dbuser, dbpass);
		try {
			// create new one
			ODocument doc = new ODocument();
			doc.fromString(new Gson().toJson(binding));
			doc.save();

		} finally {
			db.close();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.iccapps.sipserver.db.RegistrarDbProvider#updateBinding(com.iccapps.sipserver.sip.registrar.Binding)
	 */
	@Override
	public void updateBinding(Binding binding) {
		ODatabaseDocumentTx db = ODatabaseDocumentPool.global().acquire(url, dbuser, dbpass);
		try {
			// find doc
			ODocument doc = new ODocument();
			doc.fromString(new Gson().toJson(binding));
			doc.save();

		} finally {
			db.close();
		}
	}
}
