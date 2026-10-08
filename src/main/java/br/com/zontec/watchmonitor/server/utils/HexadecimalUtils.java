package br.com.zontec.watchmonitor.server.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.apache.commons.codec.binary.Hex;


public class HexadecimalUtils {

    public static boolean isHex(ByteBuf buf) {
        String hex = ByteBufUtil.hexDump(buf);
        try {
            HexFormat.of().parseHex(hex);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String hexToString(String msg) {
        try {
            byte[] bytes = HexFormat.of().parseHex(msg);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Se a string não for um hexadecimal válido, retorne a string original
            return msg;
        }
    }

}
