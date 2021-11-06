package com.nn.dns.gateway.forward.handler;

import com.nn.dns.gateway.concurrent.ThreadPools;
import com.nn.dns.gateway.forward.*;
import com.nn.dns.gateway.utils.BytebufConvertor;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Message;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @author xuxinjian
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class MultiForwardServerHandler extends ChannelInboundHandlerAdapter implements InitializingBean {

	@Autowired
	private ThreadPools threadPools;
	@Autowired
	private ForwardAnswerProcessor forwardAnswerProcessor;

	private Map<String, ForwardAnswer> answers = new ConcurrentHashMap<>();

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
	}


	@Override
	public void channelRead(ChannelHandlerContext ctx, Object obj) throws Exception {

		DatagramPacket datagramPacket = (DatagramPacket) obj;

		final byte[] answer = BytebufConvertor.toByteArray(datagramPacket.content());
		ExecutorService processExecutors = threadPools.getUdpReceiverExecutor();
		processExecutors.submit(() -> {
			try {
				final Message message = new Message(answer);
				forwardAnswerProcessor.handleAnswer(answer, message, datagramPacket.sender(), getAnswer(message));
			} catch (Throwable e) {
				log.error("forward exception ", e);
			}
		});
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
	}

	private String getKey(Message message) {
		return message.getHeader().getID() + "_"
				+ message.getQuestion().getName().toString() + "_"
				+ message.getQuestion().getType();
	}

	public void registerReceiver(Message message, ForwardAnswer forwardAnswer) {
		answers.put(getKey(message), forwardAnswer);
	}

	public ForwardAnswer getAnswer(Message message) {
		return answers.get(getKey(message));
	}

	/**
	 * Add answer to remove queue and remove when timeout.
	 * @param message
	 * @param timeOut
	 */
	public void delayRemoveAnswer(Message message, long timeOut) {
		delayRemoveQueue.add(new DelayStringKey(getKey(message), timeOut));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Thread threadForRemove = new Thread(new Runnable() {
			@Override
			public void run() {
				removeAnswerFromQueue();
			}
		});
		threadForRemove.setDaemon(true);
		threadForRemove.start();
		log.info("----------> removeAnswerFromQueue start success");
	}

	/**
	 * timeout deleted
	 * @Author xuxinjian
	 * @Date 2021/3/4
	 */
	private DelayQueue<MultiForwardServerHandler
			.DelayStringKey> delayRemoveQueue = new DelayQueue<MultiForwardServerHandler.DelayStringKey>();

	private static class DelayStringKey implements Delayed {

		private final String key;

		private final long initDelay;

		private long startTime;

		/**
		 * @param key
		 * @param initDelay in ms
		 */
		public DelayStringKey(String key, long initDelay) {
			this.startTime = System.currentTimeMillis();
			this.key = key;
			this.initDelay = initDelay;
		}

		public String getKey() {
			return key;
		}

		/**
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(Delayed o) {
			long delayA = startTime + initDelay - System.currentTimeMillis();
			long delayB = o.getDelay(TimeUnit.MILLISECONDS);
			if (delayA > delayB) {
				return 1;
			} else if (delayA < delayB) {
				return -1;
			} else {
				return 0;
			}
		}

		/**
		 *
		 * @see
		 * java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
		 */
		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(
					startTime + initDelay - System.currentTimeMillis(),
					TimeUnit.MILLISECONDS);
		}

	}

	private void removeAnswerFromQueue() {
		while (true) {
			try {
				MultiForwardServerHandler.DelayStringKey delayRemoveKey = delayRemoveQueue.take();
				ForwardAnswer forwardAnswer = answers.get(delayRemoveKey.getKey());
				if (forwardAnswer != null && forwardAnswer.getTempAnswer() != null) {
					forwardAnswer.getResponser().response(forwardAnswer.getTempAnswer().toWire());
					log.info("从队列返回---------->client:{} query：{} 状态码：{}",
							forwardAnswer.getResponser().getInDataPacket().sender(),
							forwardAnswer.getTempAnswer().getQuestion().getName(),
							forwardAnswer.getTempAnswer().toWire()[3]&7
							);
				}
				answers.remove(delayRemoveKey.getKey());
//                if (log.isDebugEnabled()) {
                    log.info("remove key " + delayRemoveKey.getKey());
//                }
			} catch (Exception e) {
				log.error("remove answer error", e);
			}
		}
	}
}
