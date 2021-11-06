package com.nn.dns.gateway.forward;

import com.nn.dns.gateway.connector.DnsUdpServerResponser;
import org.xbill.DNS.Message;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yihua.huang@dianping.com
 * @date Apr 10, 2013
 */
public class ForwardAnswer {

    private final Message query;

    private final DnsUdpServerResponser responser;

    private final long startTime;

    private AtomicInteger minOrder = new AtomicInteger(Integer.MAX_VALUE);

    /**
     * Answer that not return immediately. It will be returned when countDown<=0 or ForwardAnswer is removed.
     */
    private Message tempAnswer;

    /**
     * Number of external DNS response which not recevied yet.
     */
    private final AtomicInteger countDown;

    /**
     * @param query
     * @param responser
     */
    public ForwardAnswer(Message query, DnsUdpServerResponser responser, int initCount) {
        super();
        this.query = query;
        this.responser = responser;
        this.startTime = System.currentTimeMillis();
        this.countDown = new AtomicInteger(initCount);
    }

    public DnsUdpServerResponser getResponser() {
        return responser;
    }

    public long getStartTime() {
        return startTime;
    }

    public Message getQuery() {
        return query;
    }

    /**
     * When the DNS server' order is less than  minOrder, it will be processed.
     *
     * @param order
     * @return
     */
    public boolean shouldProcess(int order) {
        return minOrder.get() > order;
    }

    /**
     * Set the order as minOrder to prevent DNS server with larger order to be processed.
     * 确认解析顺序，根据配置的DNS服务器的顺序解析，如果有优先级高的服务器已经返回了结果，则优先级低的服务器返回的结果不会发送给客户端
     * @param order
     * @return
     */
    public boolean confirmProcess(int order) {
        int minOrderi = minOrder.get();
        while (order < minOrderi) {
            boolean b = minOrder.compareAndSet(minOrderi, order);
            if (b) {
                return true;
            } else {
                minOrderi = minOrder.get();
            }
        }
        return false;
    }

    public Message getTempAnswer() {
        return tempAnswer;
    }

    public void setTempAnswer(Message tempAnswer) {
        this.tempAnswer = tempAnswer;
    }

    public int decrCountDown() {
        return countDown.decrementAndGet();
    }

    public int getCountDown() {
        return countDown.get();
    }
}
