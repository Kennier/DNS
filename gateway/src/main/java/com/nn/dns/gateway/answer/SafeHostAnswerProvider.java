package com.nn.dns.gateway.answer;

import com.nn.dns.gateway.antipollution.SafeHostManager;
import com.nn.dns.gateway.config.DnsProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Type;

/**
 * @author yihua.huang@dianping.com
 * @date Feb 20, 2013
 */
@Component
public class SafeHostAnswerProvider implements AnswerProvider {

	@Autowired
	private SafeHostManager safeBoxService;

	@Autowired
	private DnsProperties dnsProperties;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * AnswerProvider#getAnswer(java.lang.String,
	 * int)
	 */
	@Override
	public String getAnswer(String query, int type) {
		if (!dnsProperties.isEnableSafeBox()) {
			return null;
		}
		if (type == Type.A || type == Type.AAAA) {
			return safeBoxService.get(StringUtils.removeEnd(query, "."));
		}
		return null;
	}

}
