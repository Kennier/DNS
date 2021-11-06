package com.nn.dns.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.HashSet;
import java.util.Set;


/**
 * @author xuxinjian
 */
@RefreshScope
@ConfigurationProperties(prefix = "nn.whitelist", ignoreInvalidFields = true)
public class WhiteListConfig {

    /**
     * get whitelist url
     */
    private String url;

    /**
     * token.
     */
    private String key;

    /**
     * 域名白名单
     */
    private Set<String> domains = new HashSet<String>();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Set<String> getDomains() {
        return domains;
    }

    public void setDomains(Set<String> domains) {
        this.domains = domains;
    }

    @Override
    public String toString() {
        return "WhiteListConfig{" +
                "url='" + url + '\'' +
                ", key='" + key + '\'' +
                ", domains=" + domains +
                '}';
    }
}
