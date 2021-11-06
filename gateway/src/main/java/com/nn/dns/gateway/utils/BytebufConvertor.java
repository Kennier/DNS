package com.nn.dns.gateway.utils;

import io.netty.buffer.ByteBuf;

public class BytebufConvertor {

    public static byte[] toByteArray(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes, 0, byteBuf.readableBytes());
        byteBuf.release();
        return bytes;
    }
}
