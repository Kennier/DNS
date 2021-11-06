package com.nn.dns.gateway.forward;

import com.nn.dns.gateway.config.DnsProperties;
import com.nn.dns.gateway.forward.handler.MultiForwardServerHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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
public class MultiForwardServerJp1 implements InitializingBean {

    /**
     *
     */
    private static final int dnsPackageLength = 512;

    @Autowired
    MultiForwardServerHandler multiForwardServerHandler;

    private Channel channel;

    @Autowired
    private DnsProperties dnsProperties;

    private final static int PORT_RECEIVE = 40312;

    private EventLoopGroup worker = new NioEventLoopGroup();

    /**
     * @see InitializingBean#afterPropertiesSet()
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

        List<String> ipList = new ArrayList<String>();
        NetworkInterface jp1 = NetworkInterface.getByName(dnsProperties.getNetworkCard());
        if(jp1 == null){
            log.error("jp1网卡不存在------------");
            return;
        }
        Enumeration<InetAddress> inetAddresses = jp1.getInetAddresses();
        InetAddress ip;
        while (inetAddresses.hasMoreElements()) {
            ip = inetAddresses.nextElement();
            if (null == ip || "".equals(ip)) {
                continue;
            }
            String sIP = ip.getHostAddress();
            if (sIP == null || sIP.indexOf(":") > -1) {
                continue;
            }
            ipList.add(sIP);
            System.out.println(sIP);
        }
        if (!ipList.isEmpty()) {
            channel = bootstrap.bind(ipList.get(0), PORT_RECEIVE).sync().channel();
        } else {
            log.error("jp1网卡   DNS转发服务启动失败");
        }
        log.info("DNS转发服务启动成功, 监听jp1网卡：{} port = {}", ipList.get(0), PORT_RECEIVE);
    }

    public Channel getChannel() {
        return channel;
    }
}
