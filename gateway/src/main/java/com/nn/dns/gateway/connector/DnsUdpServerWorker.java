package com.nn.dns.gateway.connector;

import com.nn.dns.gateway.container.QueryProcesser;
import com.nn.dns.gateway.context.RequestContext;
import com.nn.dns.gateway.forward.Forwarder;
import com.nn.dns.gateway.utils.ByteToBit;
import com.nn.dns.gateway.utils.BytebufConvertor;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.xbill.DNS.Message;


/**
 * @author 徐新建
 */
@Slf4j
public class DnsUdpServerWorker implements Runnable {

	private final DnsUdpServerResponser responser;

	private final DatagramPacket inDataPacket;

	private QueryProcesser queryProcesser;

	private Forwarder forwarder;


	public DnsUdpServerWorker(DatagramPacket inDataPacket,
							  QueryProcesser queryProcesser,
							  DnsUdpServerResponser responser,
							  Forwarder forwarder) {
		super();
		this.responser = responser;
		this.inDataPacket = inDataPacket;
		this.queryProcesser = queryProcesser;
		this.forwarder = forwarder;
	}

	@Override
	public void run() {
		try {
			RequestContext.setClientIps(inDataPacket.sender().getAddress().toString());

			byte[] data = BytebufConvertor.toByteArray(inDataPacket.content());
//			StringBuilder stringBuilder = new StringBuilder();
//			for (byte item: data){
//				stringBuilder.append(ByteToBit.byteToBit(item));
//			}
//			log.info("请求包修改前 bit : {} RecursionDesired byte：{}",stringBuilder.toString(),ByteToBit.byteToBit(data[2]));
			//RecursionDesired:true 初始化的时候查询包的RecursionDesired:都是false
			if(ByteToBit.byteToBit(data[2]).endsWith("0")){
				data[2] = (byte) (data[2]^1);
				log.info("请求包修改后 RecursionDesired byte：{}",ByteToBit.byteToBit(data[2]));
			}
//			StringBuilder stringBuilder2 = new StringBuilder();
//			for (byte item: data){
//				stringBuilder2.append(ByteToBit.byteToBit(item));
//			}
			byte[] response = queryProcesser.process(data);
			Message query = new Message(data);
			if (response != null) {
				responser.response(response);
				forwarder.asycRefreshCache(query.toWire(), query);
			} else {
                forwarder.forward(query.toWire(), query, responser);
			}
		} catch (Throwable e) {
			e.printStackTrace();
			log.error(
					"Error processing UDP connection from "
							+ responser.getCtx().channel().remoteAddress() + ", ", e);
		}
	}

}
