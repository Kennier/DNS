package com.nn.dns.gateway.cache;

import com.nn.dns.gateway.config.DnsProperties;
import com.nn.dns.gateway.forward.DNSHostsContainer;
import com.nn.dns.gateway.utils.SensorParamUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import com.nn.dns.gateway.utils.RecordUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author yihua.huang@dianping.com
 * @date Dec 19, 2012
 */
@Component
@Slf4j
public class CacheManager implements InitializingBean {

    @Autowired
    private DnsProperties dnsProperties;

    @Autowired
    private DNSHostsContainer dnsHostsContainer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.profiles.active}")
    private String profile;

    @Value("${nn.sensor.url}")
    private String sensorUrl;

    private volatile ExecutorService cacheSaveExecutors;

    @Qualifier("EhcacheClient")
    @Autowired
    private CacheClient cacheClient;

    public byte[] getResponseFromCache(Message query) {
        if (!dnsProperties.isUseCache()) {
            return null;
        }
        UDPPackage udpPackage;
        if(dnsProperties.isEnableRedisCache()){
            String key = "DNS:" + profile + ":" +getCacheKey(query);
            try {
                udpPackage = (UDPPackage) redisTemplate.opsForValue().get(key);
            }catch (Exception e){
                log.info("redis get cache error");
                return null;
            }
        }else {
            udpPackage = cacheClient
                    .<UDPPackage>get(getCacheKey(query));
        }
        if (udpPackage == null) {
            return null;
        }
        byte[] bytes = udpPackage.getBytes(query.getHeader().getID());
        return bytes;
    }

    public <T> boolean set(Message query, T value, int expireTime) {
        return cacheClient.set(getCacheKey(query), value, expireTime);
    }

    public <T> T get(Message query) {
        return cacheClient.get(getCacheKey(query));
    }

    public <T> boolean set(String key, T value, int expireTime) {
        return cacheClient.set(key, value, expireTime);
    }

    public <T> T get(String key) {
        return cacheClient.get(key);
    }

    public String getCacheKey(Message query) {
        return RecordUtils.recordKey(query.getQuestion());
    }

    private int minTTL(Message response) {
        return (int) Math.min(RecordUtils.maxTTL(response
                .getSectionArray(Section.ANSWER)), RecordUtils.maxTTL(response
                .getSectionArray(Section.ADDITIONAL)));
    }

    public void setNullResponseRecordToCache(String remoteAddr,String domin,Boolean allCname){
        if (dnsProperties.isUseCache()) {
            getCacheSaveExecutors().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        String keyValue = domin + ":::::" + remoteAddr;
                        if(allCname){
                            redisTemplate.opsForHash().increment("DNS:" + profile + ":AAAAA AllCnameResponseRecord ", keyValue,1L);
                        }else {
                            redisTemplate.opsForHash().increment("DNS:" + profile + ":AAAAA NULLResponseRecord ", keyValue,1L);
                        }
                        //上报神策
                        HashMap<String, Object> propertities = new HashMap<>();
                        propertities.put("speed_engine_dns_error_count",1);
                        propertities.put("speed_engine_dns_error_type", profile + ":" +keyValue+"  allCname: "+allCname.toString());
                        restTemplate.postForObject(sensorUrl,SensorParamUtil.getSensorParam("speed_engine_dns_error",propertities),String.class);
                    }catch (Throwable e) {
                        log.error("set to NullResponseRecordToCache or sensor error ", e);
                    }
                }
            });
        }
    }

//    public void setNullResponseToCache(String remoteAddr,String domin,String string){
//        if (dnsProperties.isUseCache()) {
//            getCacheSaveExecutors().execute(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        redisTemplate.opsForHash().put("DNS:" + profile + ":aaaaaaaaaaaaaaaaaaa null ",domin+":::::"+remoteAddr,string);
//                    }catch (Throwable e) {
//                        log.error("set to NullResponseRecordToCache error ", e);
//                    }
//                }
//            });
//        }
//    }

    public void setResponseToCache(final Message query, final byte[] responseBytes) {
        if (dnsProperties.isUseCache()) {
            getCacheSaveExecutors().execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        int expireTime;
                        if(dnsProperties.isEnableRedisCache()){
                            String key = "DNS:" + profile + ":" +getCacheKey(query);
                            String domain = query.getQuestion().getName().toString();
                            if(dnsHostsContainer.whiteListOrNot(domain)){
                                redisTemplate.opsForValue().set(key, new UDPPackage(
                                        responseBytes));
                            }else {
                                if (dnsProperties.getCacheExpire() > 0) {
                                    expireTime = dnsProperties.getCacheExpire();
                                    redisTemplate.opsForValue().set(key, new UDPPackage(
                                            responseBytes),expireTime, TimeUnit.SECONDS);
                                } else {
                                    redisTemplate.opsForValue().set(key, new UDPPackage(
                                            responseBytes));
                                }
                            }
                            //缓存 异步刷新有效时间
                            Message response = new Message(responseBytes);
                            redisTemplate.opsForValue()
                                    .setIfAbsent("DNS:" + profile + ":asycTime"+":"+ getCacheKey(query),1,minTTL(response)>0?minTTL(response):5,TimeUnit.SECONDS);
                        }else {
                            Message response = new Message(responseBytes);
                            if (dnsProperties.getCacheExpire() > 0) {
                                expireTime = dnsProperties.getCacheExpire();
                            } else {
                                expireTime = minTTL(response);
                            }
                            cacheClient.set(getCacheKey(query), new UDPPackage(
                                    responseBytes), expireTime);
                        }
                    } catch (Throwable e) {
                        log.error("set to cache error ", e);
                    }

                }
            });
        }

    }

    public void setResponseToRedisRecords(final Message query, String remoteAddress) {
        if (dnsProperties.isUseCache()) {
            getCacheSaveExecutors().execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        String domain = query.getQuestion().getName().toString();
                        if(dnsHostsContainer.whiteListOrNot(domain)){
                            List<String> realIps = new LinkedList<String>();
                            for (Record record : query.getSectionArray(Section.ANSWER)) {
                                realIps.add(record.rdataToString());
                            }
                            //缓存返回的ips
                            redisTemplate.opsForHash()
                                    .put("DNS:" + profile + ":realIps"+":"+ getCacheKey(query), remoteAddress,realIps.toString());
                        }
                    } catch (Throwable e) {
                        log.error("set to redisCache error ", e);
                    }

                }
            });
        }

    }

    public Map<String, String> getRedisRecords(final Message query) {
        Map<String, String> map;
        String domain = query.getQuestion().getName().toString();
        //缓存返回的ips
        map = redisTemplate.opsForHash()
                .entries("DNS:" + profile + ":realIps" + ":" + getCacheKey(query));
        return map;
    }

    /**
     * @return the cacheSaveExecutors
     */
    private ExecutorService getCacheSaveExecutors() {
        if (cacheSaveExecutors == null) {
            synchronized (this) {
                cacheSaveExecutors = Executors.newFixedThreadPool(dnsProperties.getThreadNum());
            }
        }
        return cacheSaveExecutors;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (dnsProperties.isUseCache()) {
            getCacheSaveExecutors();
        }
    }

    public void clearCache() {
        cacheClient.clearCache();
    }

}
