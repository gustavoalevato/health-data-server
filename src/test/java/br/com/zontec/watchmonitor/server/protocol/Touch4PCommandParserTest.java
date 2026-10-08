package br.com.zontec.watchmonitor.server.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.zontec.watchmonitor.server.command.AlertCommand;
import br.com.zontec.watchmonitor.server.command.AudioMessageCommand;
import br.com.zontec.watchmonitor.server.command.ConfigCommand;
import br.com.zontec.watchmonitor.server.command.LinkKeepCommand;
import br.com.zontec.watchmonitor.server.command.LocationCommand;
import br.com.zontec.watchmonitor.server.utils.Touch4PAudioMessageCodec;

class Touch4PCommandParserTest {

    private final Touch4PCommandParser parser = new Touch4PCommandParser();

    @Test
    void shouldParseLinkKeepWithMetrics() {
        LinkKeepCommand command = assertInstanceOf(LinkKeepCommand.class,
                parser.parse("[3G*8800000015*000D*LK,50,100,98]"));

        assertEquals("8800000015", command.imei());
        assertEquals(50, command.steps());
        assertEquals(100, command.rollsFrequency());
        assertEquals(98, command.batteryPercentage());
    }

    @Test
    void shouldParseRealtimeLocationUdMessage() {
        LocationCommand command = assertInstanceOf(LocationCommand.class,
                parser.parse("[3G*8800000015*00CD*UD,180916,025723,A,22.570733,N,113.8626083,E,0.00,249.5,0.0,6,100,60,0,0,00000010,7,255,460,1,9529,21809,158,0,40.7]"));

        assertEquals("8800000015", command.imei());
        assertEquals("249.5", command.direction());
        assertEquals(100, command.gsmStrength());
        assertEquals(60, command.batteryPercentage());
        assertEquals("22.570733", command.latitude().toPlainString());
        assertEquals("113.8626083", command.longitude().toPlainString());
    }

    @Test
    void shouldParseAlarmMessage() {
        AlertCommand command = assertInstanceOf(AlertCommand.class,
                parser.parse("[3G*8800000015*00CD*AL,180916,064153,A,22.570512,N,113.8623267,E,0.00,154.8,0.0,11,100,100,0,0,00100018,7,0,460,1,9529,21809,155,0,12.4]"));

        assertEquals("8800000015", command.imei());
        assertEquals("3G", command.manufacturer());
        assertEquals("00100018", command.statusBits());
        assertEquals(100, command.batteryPercentage());
    }

    @Test
    void shouldIdentifyFallDownAlarmFromStatusBits() {
        AlertCommand command = assertInstanceOf(AlertCommand.class,
                parser.parse("[3G*8800000015*010A*AL_LTE,241020,085353,V,0.0,N,0.0,E,22.0,0,-1,1,100,35,0,0,00200000,1,1,460,11,30602,125639728,100,5,4P-Touch,bc:5f:f6:1e:07:be,-55,ChinaNet-a9Ld,c4:b8:b5:c4:14:79,-53,ChinaNet-CX9r,f0:92:b4:95:3d:b9,-57,5B,08:31:8b:ae:ef:c8,-66,TP-LINK_274E,50:bd:5f:80:27:4e,-63,0.0]"));

        assertEquals("00200000", command.statusBits());
        assertTrue(command.isFallDownAlarm());
    }

    @Test
    void shouldParseConfigMessage() {
        ConfigCommand command = assertInstanceOf(ConfigCommand.class,
                parser.parse("[3G*8800000015*001C*CONFIG,TY:G75,UL:600,SY:1,CM:1]"));

        assertEquals("8800000015", command.imei());
        assertEquals("3G", command.manufacturer());
        assertEquals("TY:G75,UL:600,SY:1,CM:1", command.payload());
    }

    @Test
    void shouldParseTkAudioMessageAndUnescapeAmrPayload() {
        byte[] originalAudio = new byte[] { '#', 0x7D, '[', ']', ',', '*', 'A', 'M', 'R' };
        String escapedAudio = Touch4PAudioMessageCodec.encodeEscapedAudio(originalAudio);
        String message = "[3G*8800000015*0011*TK," + escapedAudio + "]";

        AudioMessageCommand command = assertInstanceOf(AudioMessageCommand.class, parser.parse(message));

        assertEquals("8800000015", command.imei());
        assertEquals("3G", command.manufacturer());
        assertArrayEquals(originalAudio, command.audioData());
    }
}
