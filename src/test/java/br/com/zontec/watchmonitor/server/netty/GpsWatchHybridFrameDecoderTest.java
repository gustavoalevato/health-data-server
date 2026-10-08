package br.com.zontec.watchmonitor.server.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

class GpsWatchHybridFrameDecoderTest {

    @Test
    void shouldDecodeRawAsciiFrame() {
        EmbeddedChannel channel = new EmbeddedChannel(new GpsWatchHybridFrameDecoder(4096));

        channel.writeInbound(Unpooled.copiedBuffer("IWAP00123456789012345#", StandardCharsets.UTF_8));

        ByteBuf decoded = channel.readInbound();
        assertNotNull(decoded);
        assertEquals("IWAP00123456789012345#", decoded.toString(StandardCharsets.UTF_8));
        decoded.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldDecodeHexEncodedFrame() {
        EmbeddedChannel channel = new EmbeddedChannel(new GpsWatchHybridFrameDecoder(4096));

        channel.writeInbound(Unpooled.copiedBuffer("49574150303031323334353637383930313233343523", StandardCharsets.US_ASCII));

        ByteBuf decoded = channel.readInbound();
        assertNotNull(decoded);
        assertEquals("IWAP00123456789012345#", decoded.toString(StandardCharsets.UTF_8));
        decoded.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldWaitForCompleteHexFrameBeforeDecoding() {
        EmbeddedChannel channel = new EmbeddedChannel(new GpsWatchHybridFrameDecoder(4096));

        channel.writeInbound(Unpooled.copiedBuffer("49574150303031323334", StandardCharsets.US_ASCII));
        assertNull(channel.readInbound());

        channel.writeInbound(Unpooled.copiedBuffer("353637383930313233343523", StandardCharsets.US_ASCII));

        ByteBuf decoded = channel.readInbound();
        assertNotNull(decoded);
        assertEquals("IWAP00123456789012345#", decoded.toString(StandardCharsets.UTF_8));
        decoded.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldDecodeRawTouch4PFrame() {
        EmbeddedChannel channel = new EmbeddedChannel(new GpsWatchHybridFrameDecoder(4096));

        channel.writeInbound(Unpooled.copiedBuffer("[SG*1234567890*0002*LK]", StandardCharsets.UTF_8));

        ByteBuf decoded = channel.readInbound();
        assertNotNull(decoded);
        assertEquals("[SG*1234567890*0002*LK]", decoded.toString(StandardCharsets.UTF_8));
        decoded.release();
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldDecodeHexEncodedTouch4PFrame() {
        EmbeddedChannel channel = new EmbeddedChannel(new GpsWatchHybridFrameDecoder(4096));

        channel.writeInbound(Unpooled.copiedBuffer("5B53472A313233343536373839302A303030322A4C4B5D", StandardCharsets.US_ASCII));

        ByteBuf decoded = channel.readInbound();
        assertNotNull(decoded);
        assertEquals("[SG*1234567890*0002*LK]", decoded.toString(StandardCharsets.UTF_8));
        decoded.release();
        channel.finishAndReleaseAll();
    }
}
