package com.nn.dns.gateway.forward;

import com.nn.dns.gateway.concurrent.ThreadPools;
import com.nn.dns.gateway.config.DnsProperties;
import com.nn.dns.gateway.connector.DnsUdpServerResponser;
import com.nn.dns.gateway.forward.handler.MultiForwardServerHandler;
import com.nn.dns.gateway.utils.HostUtil;
import com.nn.dns.gateway.utils.RecordUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Forward DNS query to hosts contained in {@link DNSHostsContainer}.Use the
 * same port 40311 for all UDP diagram and the instance of
 * {@link MultiUDPReceiver} will listen on the port 40311.Use wait/notify to
 * synchronize.
 * 
 * @author yihua.huang@dianping.com
 * @date Jan 16, 2013
 */
@Component
@Slf4j
public class MultiForwarder implements Forwarder {

//	private log log = log.getlog(getClass());

	@Autowired
	private MultiForwardServer multiForwardServer;

	@Autowired
	private MultiForwardServerJp1 multiForwardServerJP1;

	@Autowired
	private MultiForwardServerHandler multiForwardServerHandler;

	@Autowired
	private DNSHostsContainer dnsHostsContainer;

	@Autowired
	private DnsProperties dnsProperties;

	@Autowired
	private ThreadPools threadPools;

	@Autowired
	private RedisTemplate redisTemplate;

	@Value("${spring.profiles.active}")
	private String profile;

	public String getCacheKey(Message query) {
		return RecordUtils.recordKey(query.getQuestion());
	}

	/**
	 *  有缓存 过了异步探测有效时间
	 *
	 */
	@Override
	public void asycRefreshCache(byte[] queryBytes, Message query){
		//如果过了异步探测有效时间  进行探测
		if(dnsHostsContainer.whiteListOrNot(query.getQuestion().getName().toString()) && !redisTemplate.hasKey("DNS:" + profile + ":asycTime" + ":" + getCacheKey(query))){
			ThreadPoolExecutor asycRefreshCacheExecutor = threadPools.getAsycRefreshCacheExecutor();
			asycRefreshCacheExecutor.execute(() -> forward(queryBytes,query,null));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Forwarder#forward(byte[],
	 * org.xbill.DNS.Message)
	 */
	@Override
	public void forward(byte[] queryBytes, Message query,
			DnsUdpServerResponser responser) {
		// get address
		List<String> allAvaliableHosts = dnsHostsContainer
				.getAllAvaliableHosts(query.getQuestion().getName().toString());
		forward(queryBytes, query, allAvaliableHosts, responser);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see Forwarder#forward(byte[],
	 * org.xbill.DNS.Message, java.util.List)
	 */
	@Override
	public void forward(byte[] queryBytes, Message query,
			List<String> hosts, DnsUdpServerResponser responser) {
//		if (log.isDebugEnabled()) {
//			log.debug("forward query " + query.getQuestion().getName() + "_"
//					+ query.getHeader().getID());
//		}
		// send to all address
        int initCount = hosts.size();
        if (dnsProperties.getFakeDnsServer() != null) {
            // send fake dns query to detect dns poisoning
            hosts.add(0, dnsProperties.getFakeDnsServer());
        }
        ForwardAnswer forwardAnswer = new ForwardAnswer(query, responser, initCount);
        try {
        	//发送前注册转发回调
			multiForwardServerHandler.registerReceiver(query, forwardAnswer);
			try {
				for (String host : hosts) {
					send(queryBytes, HostUtil.resolve(host), dnsHostsContainer.whiteListOrNot(query.getQuestion().getName().toString()));
//					log.debug("forward query to host: " +  host + " for "
//							+ query.getQuestion().getName() + " - "
//							+ query.getHeader().getID());
				}
			} catch (IOException e) {
				log.error("error", e);
			}
		} finally {
        	//if query is timeout, and remove
			multiForwardServerHandler.delayRemoveAnswer(query, dnsProperties.getTimeout());
		}
	}

	private void send(byte[] queryBytes, String host, int port) throws IOException {
		send(queryBytes, InetSocketAddress.createUnresolved(host, port), false);
	}

	private void send(byte[] queryBytes, InetSocketAddress inetSocketAddress,boolean whiteListOrNot) throws IOException {
		Channel channel;
		if(whiteListOrNot){
			channel = multiForwardServerJP1.getChannel();
			log.info("JP1 发包 : 地址 {}",inetSocketAddress);
			if(channel == null){
				channel = multiForwardServer.getChannel();
                log.info("没有JP1 非JP1 发包 : 地址 {}",inetSocketAddress);
			}
		}else {
			channel = multiForwardServer.getChannel();
            log.info("非JP1 发包 : 地址 {}",inetSocketAddress);
		}
		ByteBuf byteBuf = Unpooled.copiedBuffer(queryBytes);
		try {
			channel.writeAndFlush(new DatagramPacket(byteBuf, inetSocketAddress));
		} catch (Exception exception) {
			exception.printStackTrace();
		}


	}

}
