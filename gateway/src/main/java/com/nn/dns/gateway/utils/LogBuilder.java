package com.nn.dns.gateway.utils;

/**
 * @Author 徐新建
 */
public class LogBuilder {

    private static final String LOG_PREX = "----------> ";

    public static String build(String s) {
        return LOG_PREX + s;
    }

}
