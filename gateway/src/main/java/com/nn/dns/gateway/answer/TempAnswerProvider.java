package com.nn.dns.gateway.answer;

import org.springframework.stereotype.Component;
import com.nn.dns.gateway.utils.DoubleKeyMap;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yihua.huang@dianping.com
 * @date Dec 14, 2012
 */
@Component
public class TempAnswerProvider implements AnswerProvider {

	private DoubleKeyMap<String, Integer, String> container;

	public TempAnswerProvider() {
		container = new DoubleKeyMap<String, Integer, String>(
				ConcurrentHashMap.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * AnswerProvider#getAnswer(java.lang.String,
	 * int)
	 */
	@Override
	public String getAnswer(String query, int type) {
		return container.get(query, type);
	}

	public void add(String query, int type, String answer) {
		container.put(query, type, answer);
	}

}
