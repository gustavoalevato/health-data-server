package br.com.zontec.watchmonitor.server.netty.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.zontec.watchmonitor.server.command.AudioMessageCommand;
import br.com.zontec.watchmonitor.server.command.ConfigCommand;
import br.com.zontec.watchmonitor.server.command.LocationCommand;

class Touch4PDeviceProtocolHandlerTest {

    private final Touch4PDeviceProtocolHandler handler = new Touch4PDeviceProtocolHandler();

    @Test
    void shouldBuildLinkKeepAck() {
        assertEquals("[3G*8800000015*0002*LK]", handler.loginResponse("3G", "8800000015"));
    }

    @Test
    void shouldBuildConfigAckWithResult() {
        ConfigCommand command = new ConfigCommand("8800000015", "3G", "8800000015", "TY:G75", "[3G*8800000015*000D*CONFIG,TY:G75]");

        assertEquals("[3G*8800000015*0008*CONFIG,1]", handler.acknowledge(command));
    }

    @Test
    void shouldNotAckRealtimeLocation() {
        LocationCommand command = new LocationCommand("8800000015", new BigDecimal("22.570733"), new BigDecimal("113.8626083"),
                BigDecimal.ZERO, "249.5", 60, 100,
                "[3G*8800000015*000D*UD,180916,025723,A,22.570733,N,113.8626083,E,0.00,249.5,0.0,6,100,60,0,0,00000010]");

        assertEquals("", handler.acknowledge(command));
    }

    @Test
    void shouldAckTkAudioMessageWithSuccessStatus() {
        AudioMessageCommand command = new AudioMessageCommand("8800000015", "3G", "8800000015",
                new byte[] { 'A', 'M', 'R' },
                "[3G*8800000015*0006*TK,AMR]");

        assertEquals("[3G*8800000015*0004*TK,1]", handler.acknowledge(command));
    }
}
