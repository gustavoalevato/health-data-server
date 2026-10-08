package br.com.zontec.watchmonitor.server.netty;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.ByteToMessageDecoder;

public class GpsWatchHybridFrameDecoder extends ByteToMessageDecoder {

    private static final byte GPS_FRAME_DELIMITER = (byte) '#';
    private static final byte TOUCH_FRAME_START = (byte) '[';
    private static final byte TOUCH_FRAME_DELIMITER = (byte) ']';

    private final int maxFrameLength;
    private Mode mode = Mode.UNKNOWN;

    public GpsWatchHybridFrameDecoder(int maxFrameLength) {
        this.maxFrameLength = maxFrameLength;
    }

    @Override
    protected void decode(io.netty.channel.ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!in.isReadable()) {
            return;
        }

        if (mode == Mode.UNKNOWN) {
            mode = detectMode(in);
        }

        switch (mode) {
            case GPS_RAW -> decodeRawFrame(in, out, GPS_FRAME_DELIMITER);
            case TOUCH_RAW -> decodeRawFrame(in, out, TOUCH_FRAME_DELIMITER);
            case GPS_HEX -> decodeHexFrame(in, out, "23");
            case TOUCH_HEX -> decodeHexFrame(in, out, "5D");
            case UNKNOWN -> {
            }
        }
    }

    private void decodeRawFrame(ByteBuf in, List<Object> out, byte delimiter) {
        int delimiterIndex = indexOf(in, delimiter);
        if (delimiterIndex < 0) {
            ensureFrameLength(in.readableBytes());
            return;
        }

        int frameLength = delimiterIndex - in.readerIndex() + 1;
        ensureFrameLength(frameLength);
        out.add(in.readRetainedSlice(frameLength));
        mode = Mode.UNKNOWN;
    }

    private void decodeHexFrame(ByteBuf in, List<Object> out, String hexDelimiter) {
        int frameEnd = findHexFrameEnd(in, hexDelimiter);
        if (frameEnd < 0) {
            ensureFrameLength(in.readableBytes());
            return;
        }

        int frameLength = frameEnd - in.readerIndex() + 1;
        ensureFrameLength(frameLength);

        byte[] hexBytes = new byte[frameLength];
        in.getBytes(in.readerIndex(), hexBytes);
        byte[] decoded = HexFormat.of().parseHex(new String(hexBytes, StandardCharsets.US_ASCII));
        in.skipBytes(frameLength);
        out.add(Unpooled.wrappedBuffer(decoded));
        mode = Mode.UNKNOWN;
    }

    private int findHexFrameEnd(ByteBuf in, String hexDelimiter) {
        int start = in.readerIndex();
        int readable = in.readableBytes();
        int end = start + readable;

        if ((readable & 1) != 0) {
            end--;
        }

        for (int i = start; i + 1 < end; i += 2) {
            if (matchesAsciiHexPair(in, i, hexDelimiter)) {
                return i + 1;
            }
        }
        return -1;
    }

    private Mode detectMode(ByteBuf in) {
        int start = in.readerIndex();
        int end = in.writerIndex();

        while (start < end && Character.isWhitespace(in.getByte(start))) {
            start++;
        }
        if (start >= end) {
            return Mode.UNKNOWN;
        }

        if (looksLikeAsciiPrefix(in, start, 'I', 'W')) {
            return Mode.GPS_RAW;
        }

        if (in.getByte(start) == TOUCH_FRAME_START) {
            return Mode.TOUCH_RAW;
        }

        if (looksLikeHexPrefix(in, start, "4957")) {
            return Mode.GPS_HEX;
        }

        if (looksLikeHexPrefix(in, start, "5B")) {
            return Mode.TOUCH_HEX;
        }

        for (int i = start; i < end; i++) {
            int current = in.getByte(i);
            if (!isHexCharacter(current)) {
                return inferRawModeFromPayload(in, start, i);
            }
        }
        return inferHexModeFromPayload(in, start, end);
    }

    private Mode inferRawModeFromPayload(ByteBuf in, int start, int currentIndex) {
        for (int i = start; i <= currentIndex && i < in.writerIndex(); i++) {
            if (in.getByte(i) == TOUCH_FRAME_DELIMITER) {
                return Mode.TOUCH_RAW;
            }
        }
        return Mode.GPS_RAW;
    }

    private Mode inferHexModeFromPayload(ByteBuf in, int start, int end) {
        for (int i = start; i + 1 < end; i += 2) {
            if (matchesAsciiHexPair(in, i, "5D")) {
                return Mode.TOUCH_HEX;
            }
            if (matchesAsciiHexPair(in, i, "23")) {
                return Mode.GPS_HEX;
            }
        }
        return Mode.UNKNOWN;
    }

    private boolean looksLikeAsciiPrefix(ByteBuf in, int start, char first, char second) {
        return in.writerIndex() - start >= 2
                && in.getByte(start) == first
                && in.getByte(start + 1) == second;
    }

    private boolean looksLikeHexPrefix(ByteBuf in, int start, String prefix) {
        if (in.writerIndex() - start < prefix.length()) {
            return false;
        }

        for (int i = 0; i < prefix.length(); i++) {
            if (Character.toUpperCase(in.getByte(start + i)) != prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAsciiHexPair(ByteBuf in, int index, String hexPair) {
        return Character.toUpperCase(in.getByte(index)) == hexPair.charAt(0)
                && Character.toUpperCase(in.getByte(index + 1)) == hexPair.charAt(1);
    }

    private boolean isHexCharacter(int current) {
        return (current >= '0' && current <= '9')
                || (current >= 'a' && current <= 'f')
                || (current >= 'A' && current <= 'F');
    }

    private int indexOf(ByteBuf in, byte target) {
        for (int i = in.readerIndex(); i < in.writerIndex(); i++) {
            if (in.getByte(i) == target) {
                return i;
            }
        }
        return -1;
    }

    private void ensureFrameLength(int frameLength) {
        if (frameLength > maxFrameLength) {
            throw new IllegalArgumentException("Frame length exceeds configured maximum: " + frameLength);
        }
    }

    private enum Mode {
        UNKNOWN,
        GPS_RAW,
        TOUCH_RAW,
        GPS_HEX,
        TOUCH_HEX
    }
}
