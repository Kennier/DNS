package com.nn.dns.gateway.forward;

import com.nn.dns.gateway.forward.handler.MultiForwardServerHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * @Author 徐新建
 *
 * Listen on port 40311 using reactor mode.
 *
 * @author yihua.huang@dianping.com
 * @date Jan 16, 2013
 */
@Component
@Slf4j
public class MultiForwardServer implements InitializingBean {

    /**
     *
     */
    private static final int dnsPackageLength = 512;

    @Autowired
    MultiForwardServerHandler multiForwardServerHandler;

    private Channel channel;

    private final static int PORT_RECEIVE = 40311;

    private EventLoopGroup worker = new NioEventLoopGroup();

    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(worker)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new FixedLengthFrameDecoder(1024));
//                        pipeline.addLast(new NioEventLoopGroup(), multiForwardServerHandler);
                        pipeline.addLast(multiForwardServerHandler);
                    }
                });

//        bootstrap.group(worker)
//                .channel(NioDatagramChannel.class)
//                .option(ChannelOption.SO_BROADCAST, true)
//                .handler(new LoggingHandler(LogLevel.DEBUG))
//                .handler(multiForwardServerHandler);

        String ip = System.getProperty("ip");
        if (StringUtils.isNotBlank(ip)) {
            channel = bootstrap.bind(ip, PORT_RECEIVE).sync().channel();
        } else {
            channel = bootstrap.bind(PORT_RECEIVE).sync().channel();
        }
        log.info("DNS转发服务启动成功, 监听网卡：{} port = {}", ip == null ? "localhost" : ip, PORT_RECEIVE);
    }

    public Channel getChannel() {
        return channel;
    }
}
