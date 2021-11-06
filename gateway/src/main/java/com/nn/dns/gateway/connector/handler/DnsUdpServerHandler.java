package com.nn.dns.gateway.connector.handler;

import com.nn.dns.gateway.concurrent.ThreadPools;
import com.nn.dns.gateway.connector.DnsUdpServerResponser;
import com.nn.dns.gateway.connector.DnsUdpServerWorker;
import com.nn.dns.gateway.container.QueryProcesser;
import com.nn.dns.gateway.forward.Forwarder;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;

/**
 * @author xuxinjian
 */
@Slf4j
@Component
public class DnsUdpServerHandler  extends ChannelInboundHandlerAdapter {

	@Autowired
	private ThreadPools threadPools;
	@Autowired
	private QueryProcesser queryProcesser;
	@Qualifier("multiForwarder")
	@Autowired
	private Forwarder forwarder;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object obj) throws Exception {

		ExecutorService executorService = threadPools.getMainProcessExecutor();
		DatagramPacket datagramPacket = (DatagramPacket) obj;
		executorService.execute(new DnsUdpServerWorker(datagramPacket,
				queryProcesser,
				new DnsUdpServerResponser(ctx, datagramPacket), forwarder));

	}
}
