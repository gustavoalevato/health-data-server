package br.com.zontec.watchmonitor.server.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class Touch4PAudioMessageCodecTest {

    @Test
    void shouldEncodeAndDecodeReservedBytesLosslessly() {
        byte[] payload = new byte[] { 0x7D, 0x5B, 0x5D, 0x2C, 0x2A, 'A', 'M', 'R' };

        String escaped = Touch4PAudioMessageCodec.encodeEscapedAudio(payload);
        byte[] decoded = Touch4PAudioMessageCodec.decodeEscapedAudio(escaped);

        assertArrayEquals(payload, decoded);
    }

    @Test
    void shouldRejectTruncatedEscapeSequence() {
        assertThrows(IllegalArgumentException.class, () ->
                Touch4PAudioMessageCodec.decodeEscapedAudio(new String(new byte[] { 0x7D }, java.nio.charset.StandardCharsets.ISO_8859_1)));
    }
}
