package com.nn.dns.gateway.config;

import com.alibaba.nacos.api.config.ConfigType;
import com.alibaba.nacos.api.config.annotation.NacosConfigListener;
import com.nn.dns.gateway.concurrent.ThreadPools;
import com.nn.dns.gateway.forward.DNSHostsContainer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.nn.dns.gateway.me.ReloadAble;

/**
 * @author yihua.huang@dianping.com
 * @date Dec 28, 2012
 */
@Component
@Slf4j
public class ConfigFileLoader implements InitializingBean, ReloadAble {

    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    private int versionNum = 0;

    @Autowired
    private DNSHostsContainer dnsHostsContainer;

    @Autowired
    DnsProperties dnsProperties;

    @Autowired
    private ThreadPools threadPools;

    private boolean reloadOff = false;

    /**
     * @param reloadOff the reloadOff to set
     */
    public void setReloadOff(boolean reloadOff) {
        this.reloadOff = reloadOff;
    }

    @NacosConfigListener(dataId = "nn-dns-gateway-dev.yaml", type = ConfigType.YAML)//不生效
    public void readConfig() {
        dnsHostsContainer.clearHosts();
        for (String dnsServer : dnsProperties.getServers()) {
            dnsHostsContainer.addHost(dnsServer);
        }
        dnsHostsContainer.setTimeout(dnsProperties.getTimeout());
    }

    /**
     * @see ReloadAble#reload()
     */
    @Override
    public void reload() {
        if (!reloadOff) {
            readConfig();
        }
        threadPools.resize();
    }

    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        readConfig();
    }

}
