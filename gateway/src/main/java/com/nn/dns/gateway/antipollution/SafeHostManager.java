package com.nn.dns.gateway.antipollution;

import com.nn.dns.gateway.config.DnsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.nn.dns.gateway.me.ShutDownAble;
import com.nn.dns.gateway.me.StandReadyWorker;

import java.io.*;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yihua.huang@dianping.com
 * @date Feb 20, 2013
 */
@Component
@Slf4j
public class SafeHostManager extends StandReadyWorker implements
		InitializingBean, ShutDownAble {

	@Autowired
	private DnsProperties dnsProperties;

	private Map<String, Boolean> poisons = new ConcurrentHashMap<String, Boolean>();

	private Map<String, String> answers = new ConcurrentHashMap<String, String>();

	private static final String FLUSH_CMD = "flush";

	public void flushToFile(String filename) throws IOException {
		PrintWriter writer = new PrintWriter(new File(filename));
		for (Entry<String, String> address : answers.entrySet()) {
			writer.println(address.getValue() + "\t" + address.getKey());
		}
        filename.charAt(1);
		writer.close();
	}

	public void loadFromFile(String filename) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new FileReader(
				new File(filename)));
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			line = line.trim();
			if (line.startsWith("#")) {
				break;
			}
			String[] split = line.split("\\s");
			if (split.length <= 1) {
				log.info("error record \"" + line + "\", ignored.");
			}
			answers.put(split[1], split[0]);
			poisons.put(split[1], Boolean.TRUE);
			if (log.isDebugEnabled()) {
				log.debug("load blacklist address " + line);
			}
		}
		bufferedReader.close();
	}

	public void add(String domain, String address) {
		answers.put(domain, address);
	}

	public boolean isPoisoned(String domain) {
		Boolean poisoned = poisons.get(domain);
		if (poisoned == null) {
			return false;
		}
		return poisoned;
	}

	public void setPoisoned(String domain) {
		poisons.put(domain, Boolean.TRUE);
	}

	public String get(String domain) {
		return answers.get(domain);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ShutDownAble#shutDown()
	 */
	@Override
	public void shutDown() {
		String filename = dnsProperties.getFilePath() + "/safehost";
		try {
			flushToFile(filename);
		} catch (IOException e) {
			log.warn("write to file " + filename + " error! " + e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		String filename = dnsProperties.getFilePath() + "/safehost";
		try {
			loadFromFile(filename);
		} catch (IOException e) {
			log.warn("load file " + filename + " error! " + e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * StandReady#doWhatYouShouldDo(java.lang.String)
	 */
	@Override
	public String doWhatYouShouldDo(String whatWifeSays) {
		if (FLUSH_CMD.equalsIgnoreCase(whatWifeSays)) {
			String filename = dnsProperties.getFilePath() + "/safehost";
			try {
				flushToFile(filename);
			} catch (IOException e) {
				log.warn("write to file " + filename + " error! " + e);
			}
			return "SUCCESS";
		}
		return null;
	}

}
