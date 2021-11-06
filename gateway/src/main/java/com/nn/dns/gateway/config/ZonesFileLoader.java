package com.nn.dns.gateway.config;

import com.nn.dns.gateway.forward.DNSHostsContainer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.nn.dns.gateway.answer.AnswerPatternProvider;
import com.nn.dns.gateway.answer.CustomAnswerPatternProvider;
import com.nn.dns.gateway.answer.DomainPatternsContainer;
import com.nn.dns.gateway.cache.CacheManager;
import com.nn.dns.gateway.utils.DoubleKeyMap;
import com.nn.dns.gateway.me.ReloadAble;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * @author yihua.huang@dianping.com
 * @date Dec 28, 2012
 */
@Component
@Slf4j
public class ZonesFileLoader implements InitializingBean, ReloadAble {

	@Autowired
	private DnsProperties dnsProperties;

	@Autowired
	private AnswerPatternProvider answerPatternContainer;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private CustomAnswerPatternProvider customAnswerPatternProvider;

    @Autowired
    private DNSHostsContainer dnsHostsContainer;


	public void readConfig() {
		try {
			DomainPatternsContainer domainPatternsContainer = new DomainPatternsContainer();
			DomainPatternsContainer nsDomainPatternContainer = new DomainPatternsContainer();
			DoubleKeyMap<String, Pattern, String> customAnswerPatternsTemp = new DoubleKeyMap<String, Pattern, String>(
					new ConcurrentHashMap<String, Map<Pattern, String>>(), LinkedHashMap.class);
			DoubleKeyMap<String, String, String> customAnswerTextsTemp = new DoubleKeyMap<String, String, String>(
					new ConcurrentHashMap<String, Map<String, String>>(), HashMap.class);

			for (String zone : dnsProperties.getZones()) {
				ZonesPattern zonesPattern = ZonesPattern.parse(zone);
				if (zonesPattern == null) {
					continue;
				}
				try {
					if (zonesPattern.getUserIp() == null) {
						for (Pattern pattern : zonesPattern.getPatterns()) {
							domainPatternsContainer.getDomainPatterns().put(pattern, zonesPattern.getTargetIp());
						}
						for (String text : zonesPattern.getTexts()) {
							domainPatternsContainer.getDomainTexts().put(text, zonesPattern.getTargetIp());
						}
					} else {
						for (Pattern pattern : zonesPattern.getPatterns()) {
							customAnswerPatternsTemp.put(zonesPattern.getUserIp(), pattern, zonesPattern.getTargetIp());
						}
						for (String text : zonesPattern.getTexts()) {
							customAnswerTextsTemp.put(zonesPattern.getUserIp(), text, zonesPattern.getTargetIp());
						}
					}
					log.info("read config success:\t" + zone);
				} catch (Exception e) {
					log.warn("parse config line error:\t" + zone + "\t" , e);
				}
				// For NS
				if (zone.startsWith("NS")) {
					zone = StringUtils.removeStartIgnoreCase(zone, "NS").trim();
					zonesPattern = ZonesPattern.parse(zone);
					if (zonesPattern == null) {
						continue;
					}
					try {
						for (Pattern pattern : zonesPattern.getPatterns()) {
							nsDomainPatternContainer.getDomainPatterns().put(pattern,zonesPattern.getTargetIp());
						}
						for (String text : zonesPattern.getTexts()) {
							nsDomainPatternContainer.getDomainTexts().put(text,zonesPattern.getTargetIp());
						}
						log.info("read config success:\t" + zone);
					} catch (Exception e) {
						log.warn("parse config line error:\t" + zone + "\t" + e);
					}
				}
			}

			answerPatternContainer.setDomainPatternsContainer(domainPatternsContainer);
			customAnswerPatternProvider.setDomainPatterns(customAnswerPatternsTemp);
			customAnswerPatternProvider.setDomainTexts(customAnswerTextsTemp);
			dnsHostsContainer.setDomainPatternsContainer(nsDomainPatternContainer);
		} catch (Throwable e) {
			log.warn("read config file failed:", e);
		}
	}

	/**
	 * @see ReloadAble#reload()
	 */
	@Override
	public void reload() {
		readConfig();
	}

	/**
	 * @see
	 * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		reload();
	}

}
