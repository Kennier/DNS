package com.nn.dns.gateway.forward;

import com.nn.dns.gateway.antipollution.BlackListManager;
import com.nn.dns.gateway.antipollution.SafeHostManager;
import com.nn.dns.gateway.config.DnsProperties;
import com.nn.dns.gateway.utils.ByteToBit;
import com.nn.dns.gateway.utils.RecordBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;
import com.nn.dns.gateway.cache.CacheManager;
import com.nn.dns.gateway.utils.RecordUtils;
import sun.net.util.IPAddressUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: cairne
 * Date: 13-5-19
 * Time: 下午7:28
 */
@Service
@Slf4j
public class ForwardAnswerProcessor {

    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private BlackListManager blackListManager;
    @Autowired
    private DNSHostsContainer dnsHostsContainer;

    @Autowired
    private ConnectionTimer connectionTimer;

    @Autowired
    private SafeHostManager safeBoxService;

    @Autowired
    private DnsProperties dnsProperties;

    public void handleAnswer(byte[] answer, Message message, SocketAddress remoteAddress, ForwardAnswer forwardAnswer)
            throws IOException {
        // fake dns server return an answer, it must be dns pollution
        if (dnsProperties.getFakeDnsServer() != null
                && ((InetSocketAddress)remoteAddress).getAddress().toString().contains(dnsProperties.getFakeDnsServer())) {
            addToBlacklist(message);
            String domain = StringUtils.removeEnd(message.getQuestion()
                    .getName().toString(), ".");
            safeBoxService.setPoisoned(domain);
            return;
        }
//        if (log.isTraceEnabled()) {
//            log.trace("get message from " + remoteAddress + "\n" + message);
//        }
        if (forwardAnswer == null) {
            log.info("Received messages from " + remoteAddress + " for "
                    + message.getQuestion().getName().toString()
                    + " after timeout!");
            return;
        }

        List<String> realIps = new LinkedList<String>();
        for (Record record : message.getSectionArray(Section.ANSWER)) {
            realIps.add(record.rdataToString());
        }
        log.info("----------> query：{} queryType：{} 从{}解析的真实ip:{} byte长度为：{}",
                message.getQuestion().getName(),Type.string(message.getQuestion().getType()),
                remoteAddress,
                realIps,answer.length);

        if (dnsProperties.isEnableSafeBox()) {
            answer = removeFakeAddress(message, answer);
        }

        //解析的结果都不是(A记录)ip 都是cname记录
        boolean allCname = true;

        if(realIps.size() == 0){
            allCname = false;
            //没有解析到结果
            try {
//                StringBuilder stringBuilder = new StringBuilder();
//                for (byte item: answer){
//                    stringBuilder.append(ByteToBit.byteToBit(item));
//                }
//                log.info("打印NULL结果 bit: {}",stringBuilder.toString());
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.info("client:{} 解析：{} realIps 为空 sleep 2s 失败",
                        forwardAnswer.getResponser().getInDataPacket().sender(),
                        message.getQuestion().getName());
            }
        }else {
            for (String realIp: realIps) {
                if(IPAddressUtil.isIPv4LiteralAddress(realIp) || IPAddressUtil.isIPv6LiteralAddress(realIp)){
                    allCname = false;
                    break;
                }
            }
        }

        if(allCname){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.info("client:{} 解析：{} 全是cname记录 sleep 2s 失败",
                        forwardAnswer.getResponser().getInDataPacket().sender(),
                        message.getQuestion().getName());
            }
        }

        if(forwardAnswer.getResponser() == null){
            //没有返回通道: 表示是异步探测  只为刷新缓存
            if (answer != null) {
                //缓存记录
                cacheManager.setResponseToRedisRecords(message,((InetSocketAddress)remoteAddress).getAddress().toString());
                /**
                 * 需要进行置信度判断之后刷新缓存  现在是按优先级刷新缓存
                 * 1：多源比较（交集多的）
                 * 2：引擎上报（用户反馈）的不可用ip 比较这个remoteAddress
                 * 3：httpDNS
                 * */
                Map<String, String> redisRecords = cacheManager.getRedisRecords(message);
                Map<String, Integer> requestTimes = dnsHostsContainer.getRequestTimes();
                if(redisRecords != null && redisRecords.size() > 2){
                    //1：多源比较（交集）
                    List<String> recordKeys = new ArrayList<>(redisRecords.keySet());
                    for (int i = 0; i < recordKeys.size(); i++) {
                        String key = recordKeys.get(i);
                        String value = redisRecords.get(key);
                        Set<String> collect = Arrays.stream(value.split(",")).collect(Collectors.toSet());
                        Set result = new HashSet();
                        for (int j = i+1; j < recordKeys.size(); j++) {
                            String nextKey = recordKeys.get(j);
                            String nextValue = redisRecords.get(key);
                            Set<String> nextCollect = Arrays.stream(nextValue.split(",")).collect(Collectors.toSet());
                            result.addAll(collect);
                            result.retainAll(nextCollect);
                            String keyStr = key.substring(1);
                            String nextKeyStr = nextKey.substring(1);
                            if(requestTimes.get(keyStr) != null){
                                requestTimes.put(keyStr,(-result.size()));
                            }
                            if(requestTimes.get(nextKeyStr) != null){
                                requestTimes.put(nextKeyStr,(-result.size()));
                            }
                        }
                    }
                }

                int order = dnsHostsContainer.getOrder(remoteAddress.toString());
                if (forwardAnswer.confirmProcess(order)) {
                    log.info("异步探测  刷新缓存");
                    if(realIps.size() > 0 && !allCname){
                        cacheManager.setResponseToCache(message, answer);
                    }
                }
            }
        }else {
            if (answer != null) {
                forwardAnswer.decrCountDown();
                int order = dnsHostsContainer.getOrder(remoteAddress.toString());
                if (!RecordUtils.hasAnswer(message)) {
                    forwardAnswer.setTempAnswer(message);
                    if (forwardAnswer.getCountDown() <= 0) {
                        forwardAnswer.getResponser().response(answer);
                        log.info("返回给客户端TempAnswer---------->client:{} query：{} 从{}解析的真实ip:{}  状态码：{}",
                                forwardAnswer.getResponser().getInDataPacket().sender(),
                                message.getQuestion().getName(),
                                remoteAddress,
                                realIps,answer[3]&7);
                        cacheManager.setNullResponseRecordToCache(remoteAddress.toString(),message.getQuestion().getName().toString(),allCname);
                        forwardAnswer.setTempAnswer(null);
                    }
                } else {
                    //缓存多个DNS源返回的记录
                    boolean b = dnsHostsContainer.whiteListOrNot(message.getQuestion().getName().toString());
                    log.info("域名 {} 是否在白名单：{}",message.getQuestion().getName().toString(),b);
                    //不在白名单 返回域名不存在
                    if(!b){
                        message.removeAllRecords(Section.ANSWER);
                        answer = message.toWire();
                        answer[3] = -125;//域名不存在
                        if (forwardAnswer.confirmProcess(order)) {
                            forwardAnswer.getResponser().response(answer);
                        }
                    }else {
                        cacheManager.setResponseToRedisRecords(message,((InetSocketAddress)remoteAddress).getAddress().toString());
                        if (forwardAnswer.confirmProcess(order)) {
                            forwardAnswer.getResponser().response(answer);
                            log.info("返回给客户端---------->client:{} query：{} 从{}解析的真实ip:{}  状态码：{}",
                                    forwardAnswer.getResponser().getInDataPacket().sender(),
                                    message.getQuestion().getName(),
                                    remoteAddress,
                                    realIps,answer[3]&7);
                            //记录返回给客户端为空 或者全是cname的结果
                            if(realIps.size() == 0 || allCname){
                                cacheManager.setNullResponseRecordToCache(remoteAddress.toString(),message.getQuestion().getName().toString(),allCname);
                            }else {
                                cacheManager.setResponseToCache(message, answer);
                            }
                        }
                    }
                }
            }
        }

    }

    private void addToBlacklist(Message message) {
        for (Record answer : message.getSectionArray(Section.ANSWER)) {
            String address = StringUtils.removeEnd(answer.rdataToString(), ".");
            if (!blackListManager.inBlacklist(address)) {
                log.info("detected dns poisoning, add address " + address
                        + " to blacklist");
                blackListManager.addToBlacklist(address);
            }
        }
    }

    private byte[] removeFakeAddress(Message message, byte[] bytes) {
        Record[] answers = message.getSectionArray(Section.ANSWER);
        boolean changed = false;
        for (Record answer : answers) {
            String address = StringUtils.removeEnd(answer.rdataToString(), ".");
            if ((answer.getType() == Type.A || answer.getType() == Type.AAAA)
                    && blackListManager.inBlacklist(address)) {
                if (!changed) {
                    // copy on write
                    message = (Message) message.clone();
                }
                message.removeRecord(answer, Section.ANSWER);
                changed = true;
            }
        }
        if (changed && message.getQuestion().getType() == Type.A
                && (message.getSectionArray(Section.ANSWER) == null || message
                .getSectionArray(Section.ANSWER).length == 0)
                && (message.getSectionArray(Section.ADDITIONAL) == null || message
                .getSectionArray(Section.ADDITIONAL).length == 0)
                && (message.getSectionArray(Section.AUTHORITY) == null || message
                .getSectionArray(Section.AUTHORITY).length == 0)) {
            log.info("remove message " + message.getQuestion());
            return null;
        }
        if (changed) {
            return message.toWire();
        }
        return bytes;
    }


}
