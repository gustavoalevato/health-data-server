package br.com.zontec.watchmonitor.server.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Delimiters {

	public static ByteBuf[] hash(String input) {
        return new ByteBuf[] {
                Unpooled.wrappedBuffer(input.getBytes())
        };
    }
}
