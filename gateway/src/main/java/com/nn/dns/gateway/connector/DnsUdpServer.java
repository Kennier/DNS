package com.nn.dns.gateway.connector;

import com.nn.dns.gateway.config.DnsProperties;
import com.nn.dns.gateway.connector.handler.DnsUdpServerHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * DNS服务
 * @Author 徐新建
 */
@Slf4j
@Component
public class DnsUdpServer implements ApplicationRunner {
	

    private EventLoopGroup worker = new NioEventLoopGroup();

    @Autowired
	DnsProperties dnsProperties;

    @Autowired
	DnsUdpServerHandler dnsUdpServerHandler;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
    	Bootstrap bootstrap = new Bootstrap();

		bootstrap.group(worker)
                .channel(NioDatagramChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
        		.handler(new ChannelInitializer<Channel>() {
        			@Override
        			protected void initChannel(Channel ch) throws Exception {
        				ChannelPipeline pipeline = ch.pipeline();
        				pipeline.addLast(new FixedLengthFrameDecoder(1024));
        				pipeline.addLast(dnsUdpServerHandler);
        			}
        		});

        ChannelFuture channelFuture1 = bootstrap.bind(dnsProperties.getServerPort()).sync();
        if (channelFuture1 != null && channelFuture1.isSuccess()) {
            log.info("DNS解析服务启动成功, port = {}", dnsProperties.getServerPort());
        } else {
            log.error("DNS解析服务启动失败");
        }
	}

}
