package com.iccapps.sipserver.db;

import java.util.List;
import java.util.Properties;

import com.iccapps.sipserver.sip.registrar.Binding;
import com.iccapps.sipserver.sip.registrar.SipUser;

public interface RegistrarDbProvider {

	public abstract void configure(Properties config);

	public abstract boolean isDomainSupported(String domain);

	public abstract SipUser findSipUser(String user, String domain);

	public abstract List<Binding> getBindings(String aor);

	public abstract void saveBinding(Binding binding);

	public abstract void updateBinding(Binding binding);

}