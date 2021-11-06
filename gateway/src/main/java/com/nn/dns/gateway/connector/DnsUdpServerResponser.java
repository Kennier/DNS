package com.nn.dns.gateway.connector;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

/**
 * @author xuxinjian
 */
@Slf4j
public class DnsUdpServerResponser {

	private final ChannelHandlerContext ctx;
	private final DatagramPacket inDataPacket;

	public DnsUdpServerResponser(ChannelHandlerContext ctx,
                                 DatagramPacket inDataPacket) {
		super();
		this.ctx = ctx;
		this.inDataPacket = inDataPacket;
	}

	public DatagramPacket getInDataPacket() {
		return inDataPacket;
	}

	public void response(byte[] response) {

		try {

			if (ObjectUtils.isEmpty(response)) {
				return;
			}
			try {
				ByteBuf byteBuf = Unpooled.copiedBuffer(response);
				ctx.writeAndFlush(new DatagramPacket(byteBuf, inDataPacket.sender()));
			} catch (Exception e) {
				log.error("Error sending UDP response to "
						+ ctx.channel().remoteAddress() + ", " + e);
			}

		} catch (Throwable e) {

			log.error(
					"Error processing UDP connection from "
							+ ctx.channel().remoteAddress() + ", ", e);
		}
	}
	public ChannelHandlerContext getCtx() {
		return ctx;
	}
}
