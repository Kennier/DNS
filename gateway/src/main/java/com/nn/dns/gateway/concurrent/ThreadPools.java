package com.nn.dns.gateway.concurrent;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.nn.dns.gateway.config.DnsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * @author yihua.huang@dianping.com
 * @date Apr 11, 2013
 */
@Component
@Slf4j
public class ThreadPools implements InitializingBean {

    private ThreadPoolExecutor mainProcessExecutor;
    private ThreadPoolExecutor udpReceiverExecutor;
    private ThreadPoolExecutor asycRefreshCacheExecutor;
    private int threadNum = 0;
    @Autowired
    private DnsProperties dnsProperties;

    public void resize() {
        if (threadNum != dnsProperties.getThreadNum()) {
            threadNum = dnsProperties.getThreadNum();
            log.info("Thread num changed, resize to " + threadNum);
            if (threadNum < dnsProperties.getThreadNum()) {
                mainProcessExecutor.setMaximumPoolSize(threadNum);
                mainProcessExecutor.setCorePoolSize(threadNum);
                udpReceiverExecutor.setMaximumPoolSize(threadNum);
                udpReceiverExecutor.setCorePoolSize(threadNum);
                asycRefreshCacheExecutor.setMaximumPoolSize(threadNum);
                asycRefreshCacheExecutor.setCorePoolSize(threadNum);
            } else {
                mainProcessExecutor.setCorePoolSize(threadNum);
                mainProcessExecutor.setMaximumPoolSize(threadNum);
                udpReceiverExecutor.setCorePoolSize(threadNum);
                udpReceiverExecutor.setMaximumPoolSize(threadNum);
                asycRefreshCacheExecutor.setCorePoolSize(threadNum);
                asycRefreshCacheExecutor.setMaximumPoolSize(threadNum);
            }
        }
    }

    public ExecutorService getMainProcessExecutor() {
        return mainProcessExecutor;
    }

    public ExecutorService getUdpReceiverExecutor() {
        return udpReceiverExecutor;
    }

    public ThreadPoolExecutor getAsycRefreshCacheExecutor() {
        return asycRefreshCacheExecutor;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        threadNum = dnsProperties.getThreadNum();
        log.info("----------> 初始化线程池, threadNum:{}", threadNum);

        ThreadFactory mainProcessThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("main-process-%d").build();
        mainProcessExecutor = new ThreadPoolExecutor(threadNum, threadNum, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), mainProcessThreadFactory);

        ThreadFactory udpReceiverThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("udp-receiver-%d").build();
        udpReceiverExecutor = new ThreadPoolExecutor(threadNum, threadNum, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), udpReceiverThreadFactory);

        ThreadFactory asycRefreshCacheThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("asyc-refresh-%d").build();
        asycRefreshCacheExecutor = new ThreadPoolExecutor(threadNum, threadNum, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), asycRefreshCacheThreadFactory);
    }
}
