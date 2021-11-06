package com.nn.dns.gateway.utils;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 *
 * @Author 徐新建
 */
public class HostUtil {

    /**
     * 默认远程DNS服务器的端口为53
     */
    private static final int DEFAULT_REMOTE_DNS_SERVER_PORT = 53;

    private static final String SEPARATOR = ":";

    /**
     * 解析主机地址是否包含端口
     * @param host
     * @return java.net.InetSocketAddress
     */
    public static InetSocketAddress resolve (String host) {
        if (host.indexOf(SEPARATOR) != -1) {
            String[] args = host.split(SEPARATOR);
            return new InetSocketAddress(args[0], Integer.valueOf(args[1]));
        }
        return new InetSocketAddress(host, DEFAULT_REMOTE_DNS_SERVER_PORT);
    }

    /**
     * 匹配白名单
     * @Author 徐新建
     * @Date 2021/3/8 
     * @param whiteList
	 * @param domain
     * @return boolean
     */
    public static boolean matchWhiteList(Collection<String> whiteList, String domain) {

        //替换末尾的.
        if (domain.endsWith(".")) {
            domain = domain.substring(0, domain.length() - 1);
        }

        for (String str : whiteList) {
            if (str.equals(domain) || domain.matches(getRegex(str))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成域名的表达式 如："^.*.qq.com$
     * @Author 徐新建
     * @Date 2021/3/8
     * @param domain
     * @return java.lang.String
     */
    private static String getRegex(String domain) {
        String s = "^.*.";
        s += domain;
        s += "$";
        return s;
    }
}
