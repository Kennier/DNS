package com.nn.dns.gateway.config;

import com.nn.dns.gateway.forward.Forwarder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 白名单刷新
 * @Author 徐新建
 * @Date 2021/3/3
 */
@Component
@Slf4j
public class WhiteListRefresher implements InitializingBean {

    @Autowired
    private WhiteListConfig whiteListConfig;
    @Autowired
    DnsProperties dnsProperties;
    @Qualifier("multiForwarder")
    @Autowired
    private Forwarder forwarder;

    @Autowired
    DNSProtocol dnsProtocol;

    @Autowired
    private RestTemplate restTemplate;

    private ScheduledExecutorService scheduledExecutorService = Executors
            .newScheduledThreadPool(1);

    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() {

        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    try {
                        String url = whiteListConfig.getUrl() + "?key=" + whiteListConfig.getKey();
                        String whiteListStr = restTemplate.getForObject(url, String.class);
                        if (StringUtils.isNotBlank(whiteListStr)) {
                            String[] domains = whiteListStr.split("\n");
                            if (!ObjectUtils.isEmpty(domains)) {
                                Set<String> newWhiteList = new HashSet<String>(domains.length);
                                newWhiteList.addAll(Arrays.asList(domains));
                                whiteListConfig.setDomains(newWhiteList);
                                log.debug("----------> 白名单长度：" + newWhiteList.size());
                            }
                        }
                    } catch (Exception e) {
                        log.error("同步白名单失败", e);
                    }
                }
            }
        }, 1, 30, TimeUnit.SECONDS);

        //定时任务  异步探测
//        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
//            @Override
//            public void run() {
//                synchronized (this) {
//                    try {
//                       whiteListConfig.getDomains().forEach(item -> {
//                           byte[] dnsPacketBuffer = dnsProtocol.getDnsPacketBuffer(item);
//                           try {
//                               forwarder.forward(dnsPacketBuffer,new Message(dnsPacketBuffer),null);
//                               Thread.sleep(30000);
//                           } catch (Exception e) {
//                               log.error("定时任务  异步探测 失败", e);
//                           }
//                       });
//                    } catch (Exception e) {
//                        log.error("定时任务  异步探测 失败", e);
//                    }
//                }
//            }
//        }, 1, 30, TimeUnit.MINUTES);
    }

}
