package com.nn.dns.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import com.nn.dns.gateway.utils.SpringLocator;

import java.net.UnknownHostException;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
@ConfigurationPropertiesScan
@EnableDiscoveryClient
//@EnableDiscoveryClient
//@Import(SpringContextUtils.class)
//@ComponentScan(basePackages = {"com.leigod", "com.nn", "us"},
//        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {CacheAutoConfigure.class, RedisLockUtil.class}))
public class NnDnsGatewayApplication{

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext application = SpringApplication.run(NnDnsGatewayApplication.class, args);
        SpringLocator.applicationContext = application;
    }
}