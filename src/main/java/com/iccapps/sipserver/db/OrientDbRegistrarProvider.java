package com.iccapps.sipserver.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gson.Gson;
import com.iccapps.sipserver.sip.registrar.Binding;
import com.iccapps.sipserver.sip.registrar.SipUser;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class OrientDbRegistrarProvider {
	
	private static final String ORIENTDB_URL = "orientdb.url";
	private static final String ORIENTDB_USER = "orientdb.username";
	private static final String ORIENTDB_PASS = "orientdb.password";
	
	private String url;
	private String dbuser;
	private String dbpass;

	public OrientDbRegistrarProvider() {
	}
	
	public void configure(Properties config) {
		url = config.getProperty(ORIENTDB_URL, "remote:localhost/registrar");
		dbuser = config.getProperty(ORIENTDB_USER, "guest");
		dbpass = config.getProperty(ORIENTDB_PASS, "guest");
	}

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
