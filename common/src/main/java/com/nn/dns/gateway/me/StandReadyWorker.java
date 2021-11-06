package com.nn.dns.gateway.me;

import java.util.List;

/**
 * @author yihua.huang@dianping.com
 * @date Dec 19, 2012
 */
public abstract class StandReadyWorker implements StandReady {

	/*
	 * (non-Javadoc)
	 * 
	 * @see StandReady#whatKindOfJobWillYouDo()
	 */
	@Override
	public Class<? extends JobTodo> whatKindOfJobWillYouDo() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see StandReady#setJobs(java.util.List)
	 */
	@Override
	public void setJobs(List<? extends JobTodo> jobs) {

	}

}
