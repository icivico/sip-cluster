package com.iccapps.sipserver.cluster.hz;

import org.apache.log4j.Logger;

import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;

public class MemberObserver implements MembershipListener {

	private static Logger logger = Logger.getLogger(MemberObserver.class);
	
	public MemberObserver() {}
	
	@Override
	public void memberAdded(MembershipEvent arg0) {
		logger.debug("Member added " + arg0.getMember().getUuid());
		
	}

	@Override
	public void memberRemoved(MembershipEvent ev) {
		logger.debug("Member removed " + ev.getMember().getUuid());
		ClusterImpl.getInstance().handleServerFailure(ev.getMember());
	}

}
