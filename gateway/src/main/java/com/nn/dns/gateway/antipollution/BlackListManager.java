package com.nn.dns.gateway.antipollution;

import com.nn.dns.gateway.config.DnsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Message;
import com.nn.dns.gateway.me.ShutDownAble;
import com.nn.dns.gateway.me.StandReadyWorker;

import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author yihua.huang@dianping.com
 * @date Feb 19, 2013
 */
@Component
@Slf4j
public class BlackListManager extends StandReadyWorker implements
		InitializingBean, ShutDownAble {

	@Autowired
    DnsProperties dnsProperties;

	private Map<String, Set<String>> invalidAddresses = new ConcurrentHashMap<String, Set<String>>();

	private Set<String> blacklist = new HashSet<String>();

	private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private static final String FLUSH_CMD = "flush";

	public void registerInvalidAddress(Message query, String address) {
		String questionName = query.getQuestion().getName().toString();
		log.info("register error address " + address + " for  query "
				+ questionName);
		Set<String> questionNames = invalidAddresses.get(address);
		if (questionNames == null) {
			questionNames = new HashSet<String>();
			invalidAddresses.put(address, questionNames);
		}
		questionNames.add(questionName);
		if (questionNames.size() >= 2) {
			try {
				readWriteLock.writeLock().lock();
				blacklist.add(address);
			} finally {
				readWriteLock.writeLock().unlock();
			}
		}
	}

	public void addToBlacklist(String address) {
		try {
			readWriteLock.writeLock().lock();
			blacklist.add(address);
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	public boolean inBlacklist(String address) {
		try {
			readWriteLock.readLock().lock();
			return blacklist.contains(address);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	public void flushToFile(String filename) throws IOException {
		PrintWriter writer = new PrintWriter(new File(filename));
		for (String address : blacklist) {
			writer.println(address);
		}
		writer.close();
	}

	public void loadFromFile(String filename) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new FileReader(
				new File(filename)));
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			line = line.trim();
			if (log.isDebugEnabled()) {
				log.debug("load blacklist address " + line);
			}
			blacklist.add(line);
		}
		bufferedReader.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ShutDownAble#shutDown()
	 */
	@Override
	public void shutDown() {
		String filename = dnsProperties.getFilePath() + "/blacklist";
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
		String filename = dnsProperties.getFilePath() + "/blacklist";
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
			String filename = dnsProperties.getFilePath() + "/blacklist";
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
