package com.nn.dns.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.List;


/**
 * @author xuxinjian
 */
@RefreshScope
@ConfigurationProperties(prefix = "nn.dns", ignoreInvalidFields = true)
public class DnsProperties {

    public static final int DEFAULT_DNS_SERVER_PORT = 53;

    public final static long DEFAULT_TTL = 2000;

    public final static int DEFAULT_DNS_TIMEOUT = 2000;

    public static final String DEFAULT_FILE_PATH = "/usr/local/nn/dns/";

    public final static int DEFAULT_MX_PRIORY = 10;

    public final static int ASYNC_REFRESH_TIME = 10;

    private String filePath = DEFAULT_FILE_PATH;


    private int serverPort = DEFAULT_DNS_SERVER_PORT;

    /**
     * External dns. You can add more reliable dns.
     */
    private List<String> servers;

    /**
     * 不在白名单时是否启用备用DNS服务器
     */
    private boolean useSlaveServer = true;
    /**
     * if is not in whitelist then use slave dns servers
     */
    private List<String> slaveServers;
    /**
     * Timeout of external dns in millisecond.
     */
    private long timeout = DEFAULT_DNS_TIMEOUT;

    private long ttl = DEFAULT_TTL;

    private int mxPriory = DEFAULT_MX_PRIORY;

    public int getAsyncRefreshTime() {
        return asyncRefreshTime;
    }

    public void setAsyncRefreshTime(int asyncRefreshTime) {
        this.asyncRefreshTime = asyncRefreshTime;
    }

    private int asyncRefreshTime = ASYNC_REFRESH_TIME;


    /**
     * Whether use ehcache for external dns records.When it is changed, the program must be restarted to take effort.
     */
    private boolean useCache = false;

    public boolean isEnableRedisCache() {
        return enableRedisCache;
    }

    public void setEnableRedisCache(boolean enableRedisCache) {
        this.enableRedisCache = enableRedisCache;
    }

    /**
     * Whether enable redis cache for external dns records.
     */
    private boolean enableRedisCache = false;

    /**
     * Cache expire time in seconds. Use ttl of respective DNS answer as default.
     */
    private int cacheExpire;

    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    private int versionNum;

    /**
     * fake dns server.Used to detect dns poison.
     */
    private String fakeDnsServer;

    /**
     * networkCard 网卡
     */
    private String networkCard;

    private boolean enableSafeBox;

    /**
     * Num of threads for process request. One thread is enough for local usage. Set it to 10 if you use it as a server for many users.
     */
    private int threadNum;

    /**
     * 自定义域名支持，类型hosts
     * ex. </br>
     * 127.0.0.1 localhost
     */
    private List<String> zones;

    public String getNetworkCard() {
        return networkCard;
    }

    public void setNetworkCard(String networkCard) {
        this.networkCard = networkCard;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<String> getSlaveServers() {
        return slaveServers;
    }

    public void setSlaveServers(List<String> slaveServers) {
        this.slaveServers = slaveServers;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public int getMxPriory() {
        return mxPriory;
    }

    public void setMxPriory(int mxPriory) {
        this.mxPriory = mxPriory;
    }

    public boolean isUseCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public int getCacheExpire() {
        return cacheExpire;
    }

    public void setCacheExpire(int cacheExpire) {
        this.cacheExpire = cacheExpire;
    }

    public String getFakeDnsServer() {
        return fakeDnsServer;
    }

    public void setFakeDnsServer(String fakeDnsServer) {
        this.fakeDnsServer = fakeDnsServer;
    }

    public boolean isEnableSafeBox() {
        return enableSafeBox;
    }

    public void setEnableSafeBox(boolean enableSafeBox) {
        this.enableSafeBox = enableSafeBox;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public List<String> getZones() {
        return zones;
    }

    public void setZones(List<String> zones) {
        this.zones = zones;
    }

    public boolean isUseSlaveServer() {
        return useSlaveServer;
    }

    public void setUseSlaveServer(boolean useSlaveServer) {
        this.useSlaveServer = useSlaveServer;
    }

    @Override
    public String toString() {
        return "DnsProperties{" +
                "filePath='" + filePath + '\'' +
                ", servers=" + servers +
                ", slaveServers=" + slaveServers +
                ", timeout=" + timeout +
                ", ttl=" + ttl +
                ", mxPriory=" + mxPriory +
                ", useCache=" + useCache +
                ", cacheExpire=" + cacheExpire +
                ", fakeDnsServer='" + fakeDnsServer + '\'' +
                ", enableSafeBox=" + enableSafeBox +
                ", threadNum=" + threadNum +
                ", zones=" + zones +
                '}';
    }
}
