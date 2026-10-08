package br.com.zontec.watchmonitor.server.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class Touch4PAudioMessageCodec {

    private static final int ESCAPE = 0x7D;

    private Touch4PAudioMessageCodec() {
    }

    public static byte[] decodeEscapedAudio(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload de audio nao informado.");
        }

        byte[] encoded = payload.getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream decoded = new ByteArrayOutputStream(encoded.length);

        for (int i = 0; i < encoded.length; i++) {
            int current = encoded[i] & 0xFF;
            if (current != ESCAPE) {
                decoded.write(current);
                continue;
            }

            if (i + 1 >= encoded.length) {
                throw new IllegalArgumentException("Sequencia de escape TK truncada.");
            }

            int next = encoded[++i] & 0xFF;
            decoded.write(switch (next) {
                case 0x01 -> 0x7D;
                case 0x02 -> 0x5B;
                case 0x03 -> 0x5D;
                case 0x04 -> 0x2C;
                case 0x05 -> 0x2A;
                default -> throw new IllegalArgumentException("Sequencia de escape TK invalida: 0x7D 0x"
                        + String.format("%02X", next));
            });
        }

        return decoded.toByteArray();
    }

    public static String encodeEscapedAudio(byte[] payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload de audio nao informado.");
        }

        ByteArrayOutputStream encoded = new ByteArrayOutputStream(payload.length);
        for (byte value : payload) {
            int current = value & 0xFF;
            switch (current) {
                case 0x7D -> {
                    encoded.write(0x7D);
                    encoded.write(0x01);
                }
                case 0x5B -> {
                    encoded.write(0x7D);
                    encoded.write(0x02);
                }
                case 0x5D -> {
                    encoded.write(0x7D);
                    encoded.write(0x03);
                }
                case 0x2C -> {
                    encoded.write(0x7D);
                    encoded.write(0x04);
                }
                case 0x2A -> {
                    encoded.write(0x7D);
                    encoded.write(0x05);
                }
                default -> encoded.write(current);
            }
        }

        return encoded.toString(StandardCharsets.ISO_8859_1);
    }
}
