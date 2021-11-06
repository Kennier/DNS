package com.nn.dns.gateway.cache;

import com.nn.dns.gateway.config.DnsProperties;
import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import com.nn.dns.gateway.me.ShutDownAble;
import com.nn.dns.gateway.me.StandReadyWorker;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author yihua.huang@dianping.com
 * @date Dec 19, 2012
 */
@Slf4j
@Component("EhcacheClient")
public class EhcacheClient extends StandReadyWorker implements CacheClient,
        InitializingBean, ShutDownAble {

    @Autowired
    private DnsProperties dnsProperties;

    private static volatile CacheManager manager;

    private static final String CACHE_NAME = "DNS";

    private static final String CACHE_CONF = "ehcache.xml";

    private static final String CLEAR = "clear_cache";

    private static final String DUMP = "dump_cache";

    private static final String STAT = "stat_cache";

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    /**
     * (non-Jsdoc)
     *
     * @see com.cacheClient.mail.postman.cache.service.impl.CacheClient#init()
     */
    @Override
    public void init() {
        ClassPathResource classPathResource = new ClassPathResource(CACHE_CONF);
        InputStream inputStream = null;
        try {
            inputStream = classPathResource.getInputStream();
            if (manager == null) {
                synchronized (EhcacheClient.class) {
                    if (manager == null) {
                        manager = new CacheManager(inputStream);
                        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
                            @Override
                            public void run() {
                                if (log.isDebugEnabled()) {
                                    log.debug("start to flush cache to disk");
                                }
                                try {
                                    Cache cache = manager.getCache(CACHE_NAME);
                                    cache.flush();
                                } catch (Exception e) {
                                    log.warn("flush cache error!", e);
                                }
                            }
                        }, 1, 1, TimeUnit.MINUTES);
                    }
                }
            }
        } catch (Exception e) {
            log.error("init ehcache error!", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.warn("close error", e);
            }
            inputStream = null;
        }
    }

    /**
     * (non-Jsdoc)
     *
     * @see com.dianping.mail.postman.cache.service.CacheService#get(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key) {
        if (manager == null || key == null) {
            return null;
        }
        if (manager == null) {
            return null;
        }
        Cache cache = manager.getCache(CACHE_NAME);
        Element element = cache.get(key);
        if (element == null) {
            return null;
        }
        T value = (T) element.getObjectValue();
        return value;
    }

    /**
     * (non-Jsdoc)
     *
     * @see com.dianping.mail.postman.cache.service.CacheService#set(java.lang.String,
     *      java.lang.Object, int)
     */
    @Override
    public <T> boolean set(String key, T value, int expireTime) {
        if (!dnsProperties.isUseCache()) {
            return false;
        }
        if (key == null || value == null) {
            throw new IllegalArgumentException(
                    "key and value should not be null");
        }
        if (manager == null) {
            return false;
        }
        Cache cache = manager.getCache(CACHE_NAME);
        Element element = new Element(key, value, Boolean.FALSE, 0, expireTime);
        cache.put(element);
        return true;
    }

    /**
     * telnet control for caches
     * @param whatWifeSays
     * @return
     */
    @Override
    public String doWhatYouShouldDo(String whatWifeSays) {
        if (manager == null) {
            return "CACHE NOT USED";
        }
        if (DUMP.equalsIgnoreCase(whatWifeSays)) {
            final String dumpFilename = dnsProperties.getFilePath() + "/cache.dump";
            Cache cache = manager.getCache(CACHE_NAME);
            List<String> keys = (List<String>) cache.getKeys();
            try {
                PrintWriter writer = new PrintWriter(new FileWriter(dumpFilename));
                for (String key : keys) {
                    writer.println(key + "\t" + cache.get(key));
                }
                writer.close();
            } catch (IOException e) {
                log.error("dumpfile error", e);
            }
            return keys.size() + "_caches_are_dumped_to_file_'" + dumpFilename + "'";
        } else if (CLEAR.equalsIgnoreCase(whatWifeSays)) {
            clearCache();
            return "REMOVE SUCCESS";
        } else if (STAT.equalsIgnoreCase(whatWifeSays)) {
            Cache cache = manager.getCache(CACHE_NAME);
            return cache.getSize()+" records in cache, try 'cache dump' to more info";
        }
        if (whatWifeSays.startsWith(CLEAR)) {
            String[] split = whatWifeSays.split(":");
            if (split.length >= 2) {
                String address = split[1];
                if (!address.endsWith(".")) {
                    address += ".";
                }
                String type = "A";
                if (split.length > 2) {
                    type = split[2];
                }
                String key = address + " " + type;
                if (manager == null) {
                    return "CACHE NOT USED";
                }
                Cache cache = manager.getCache(CACHE_NAME);
                if (cache.get(key) == null) {
                    return "KEY " + key + " NOT EXIST";
                }
                cache.remove(key);
                return "REMOVE SUCCESS";
            }
        }
        return null;
    }

    @Override
    public void clearCache() {
        if (manager==null){
            return;
        }
        Cache cache = manager.getCache(CACHE_NAME);
        if (cache==null){
            return;
        }
        @SuppressWarnings("unchecked")
        List<String> keys = cache.getKeys();
        log.info(keys.size() + " cached records cleared");
        if (log.isDebugEnabled()) {
            log.debug("[" + StringUtils.join(keys, ",") + "]");
        }
        cache.removeAll();
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
            Thread thread = new Thread() {
                @Override
                public void run() {
                    init();
                }
            };
            thread.setDaemon(true);
            thread.start();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see ShutDownAble#shutDown()
     */
    @Override
    public void shutDown() {
        try {
            Cache cache = manager.getCache(CACHE_NAME);
            cache.flush();
            manager.shutdown();
            log.info("flush cache to disk success!");
        } catch (Exception e) {
            log.warn("flush cache error!", e);
        }
    }

}
