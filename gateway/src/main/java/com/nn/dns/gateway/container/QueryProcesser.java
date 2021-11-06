package com.nn.dns.gateway.container;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Message;
import com.nn.dns.gateway.cache.CacheManager;
import com.nn.dns.gateway.context.RequestContext;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Main logic of blackhole.<br/>
 * Process the DNS query and return the answer.
 *
 * @author yihua.huang@dianping.com
 * @date Dec 14, 2012
 */
@Component
@Slf4j
public class QueryProcesser {

    @Autowired
    private HandlerManager handlerManager;

    @Autowired
    private CacheManager cacheManager;

    public byte[] process(byte[] queryData) throws IOException {
        Message query = new Message(queryData);
//        log.info("----------> ip：{}请求解析域名：{}", RequestContext.getClientIp(), query.getQuestion().getName());
        MessageWrapper responseMessage = new MessageWrapper(new Message(query
                .getHeader().getID()));
        for (Handler handler : handlerManager.getPreHandlers()) {
            boolean handle = handler.handle(new MessageWrapper(query),
                    responseMessage);
            if (!handle) {
                break;
            }
        }
        byte[] response = null;
        if (responseMessage.hasRecord()) {
            response = responseMessage.getMessage().toWire();
            return response;
        }

        byte[] cache = cacheManager.getResponseFromCache(query);
        if (cache != null) {
            Message message = new Message(cache);
            List<String> realIps = new LinkedList<String>();
            for (Record record : message.getSectionArray(Section.ANSWER)) {
                realIps.add(record.rdataToString());
            }
            log.info("----------> client:{} query:{} cache result:{} 状态码：{}", RequestContext.getClientIp(), query.getQuestion().getName(), realIps, cache[3]&7);
            return cache;
        } else {
            for (Handler handler : handlerManager.getPostHandlers()) {
                boolean handle = handler.handle(new MessageWrapper(query),
                        responseMessage);
                if (!handle) {
                    break;
                }
            }
            if (responseMessage.hasRecord()) {
                response = responseMessage.getMessage().toWire();
                return response;
            } else {
                return null;
            }
        }
    }
}
