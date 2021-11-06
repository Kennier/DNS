package com.nn.dns.gateway.forward;

import com.nn.dns.gateway.connector.DnsUdpServerResponser;
import org.xbill.DNS.Message;

import java.util.List;

/**
 * @author yihua.huang@dianping.com
 * @date Jan 16, 2013
 */
public interface Forwarder {

	public void asycRefreshCache(final byte[] queryBytes, Message query);

	/**
	 * Forward query bytes to external DNS host(s) and get a valid DNS answer.
	 * 
	 * @param queryBytes
	 * @param query
	 * @return
	 */
	public void forward(final byte[] queryBytes, Message query,
						DnsUdpServerResponser responser);

	/**
	 * Forward query bytes to external DNS host(s) and get a valid DNS answer.
	 * 
	 * @param queryBytes
	 * @param query
	 * @return
	 */
	public void forward(final byte[] queryBytes, Message query,
			List<String> hosts, DnsUdpServerResponser responser);

}
