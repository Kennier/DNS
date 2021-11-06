package com.nn.dns.gateway.forward;

import com.nn.dns.gateway.config.ConfigFileLoader;
import com.nn.dns.gateway.config.DnsProperties;
import com.nn.dns.gateway.config.WhiteListConfig;
import com.nn.dns.gateway.utils.HostUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.nn.dns.gateway.answer.DomainPatternsContainer;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yihua.huang@dianping.com
 * @date Dec 21, 2012
 */
@Component
@Slf4j
public class DNSHostsContainer {

	@Autowired
	WhiteListConfig whiteListConfig;
	@Autowired
	DnsProperties dnsProperties;
	@Autowired
	ConfigFileLoader configFileLoader;

	private DomainPatternsContainer domainPatternsContainer;

	private long timeout = 3000;

	private int order;

	public Map<String, Integer> getRequestTimes() {
		return requestTimes;
	}

	private Map<String, Integer> requestTimes = new ConcurrentHashMap<>();

	public void clearHosts() {
		requestTimes = new ConcurrentHashMap<String, Integer>();
		order = 0;
	}

	public void addHost(String address) {
		requestTimes.put(address, order++);
		log.info("add dns address " + address);
	}

	public int getOrder(String socketAddress) {
		Integer order = requestTimes.get(socketAddress);
		return order == null ? 0 : order;
	}

	/**
	 * @param timeout
	 *            the timeout to set
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public boolean whiteListOrNot(String domain){
		return HostUtil.matchWhiteList(whiteListConfig.getDomains(), domain);
	}

	public List<String> getAllAvaliableHosts(String domain) {

		String ip = domainPatternsContainer.getIp(domain);
		if (ip != null) {
            List<String> socketAddresses = new ArrayList<String>();
            socketAddresses.add(ip);
            return socketAddresses;
		}

		//不在白名单且配置了备用DNS服务器，则走备用
		if (!whiteListOrNot(domain)
				&& dnsProperties.isUseSlaveServer()
			&& !ObjectUtils.isEmpty(dnsProperties.getSlaveServers())) {
			List<String> socketAddresses = new ArrayList<>();
			dnsProperties.getSlaveServers().forEach(e ->{
				socketAddresses.add(e);
			});
			return socketAddresses;
		}

		List<String> results = new ArrayList<String>();
		if(dnsProperties.getVersionNum() > configFileLoader.getVersionNum()){
			configFileLoader.readConfig();
			configFileLoader.setVersionNum(dnsProperties.getVersionNum());
		}
		Iterator<Entry<String, Integer>> iterator = requestTimes.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Integer> next = iterator.next();
			results.add(next.getKey());
		}
		return results;
	}

	public void setDomainPatternsContainer(DomainPatternsContainer domainPatternsContainer) {
		this.domainPatternsContainer = domainPatternsContainer;
	}
}
