package br.com.zontec.watchmonitor.server.protocol.outbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import br.com.zontec.watchmonitor.server.api.dto.DeviceCommandAction;
import br.com.zontec.watchmonitor.server.api.dto.DeviceCommandRequest;
import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.enums.ParserModel;

class Touch4POutboundCommandBuilderTest {

    private final Touch4POutboundCommandBuilder builder = new Touch4POutboundCommandBuilder("3G");

    @Test
    void shouldWrapRawPayloadAsTouch4PEnvelope() {
        Device device = new Device();
        device.setImei("123458800000015");
        device.setParserModel(ParserModel.TOUCH_4P.name());

        assertEquals("[3G*8800000015*0002*CR]", builder.buildCommand(device, "CR"));
    }

    @Test
    void shouldPreserveFullTouch4PFrame() {
        Device device = new Device();
        device.setImei("123458800000015");
        device.setParserModel(ParserModel.TOUCH_4P.name());

        assertEquals("[3G*8800000015*0002*CR]", builder.buildCommand(device, "[3G*8800000015*0002*CR]"));
    }

    @Test
    void shouldBuildLocateCommand() {
        Device device = new Device();
        device.setImei("123458800000015");
        device.setParserModel(ParserModel.TOUCH_4P.name());

        assertEquals("[3G*8800000015*0002*CR]", builder.buildLocateCommand(device));
    }

    @Test
    void shouldBuildTypedUploadCommand() {
        Device device = new Device();
        device.setImei("123458800000015");
        device.setParserModel(ParserModel.TOUCH_4P.name());

        DeviceCommandRequest request = new DeviceCommandRequest(DeviceCommandAction.SET_UPLOAD_INTERVAL,
                null, 600, null, null, null, null, null, null, false, false, 600, null);

        assertEquals("[3G*8800000015*000A*UPLOAD,600]", builder.buildCommand(device, request));
    }

    @Test
    void shouldBuildTypedCallMonitorCenterAndApnCommands() {
        Device device = new Device();
        device.setImei("123458800000015");
        device.setParserModel(ParserModel.TOUCH_4P.name());

        assertEquals("[3G*8800000015*0010*CALL,11999999999]", builder.buildCommand(device,
                new DeviceCommandRequest(DeviceCommandAction.CALL, null, null, "11999999999", null, null, null, null, null, false, false, null, null)));
        assertEquals("[3G*8800000015*0013*MONITOR,11999999999]", builder.buildCommand(device,
                new DeviceCommandRequest(DeviceCommandAction.MONITOR, null, null, "11999999999", null, null, null, null, null, false, false, null, null)));
        assertEquals("[3G*8800000015*0012*CENTER,11999999999]", builder.buildCommand(device,
                new DeviceCommandRequest(DeviceCommandAction.CENTER, null, null, null, "11999999999", null, null, null, null, false, false, null, null)));
        assertEquals("[3G*8800000015*0011*APN,cmnet,,,72405]", builder.buildCommand(device,
                new DeviceCommandRequest(DeviceCommandAction.APN, null, null, null, null, "cmnet", "", "", "72405", false, false, null, null)));
    }

    @Test
    void shouldRejectInvalidUploadInterval() {
        Device device = new Device();
        device.setImei("123458800000015");
        device.setParserModel(ParserModel.TOUCH_4P.name());

        assertThrows(IllegalArgumentException.class, () -> builder.buildCommand(device,
                new DeviceCommandRequest(DeviceCommandAction.SET_UPLOAD_INTERVAL, null, 30, null, null, null, null, null, null, false, false, null, null)));
    }

    @Test
    void shouldBuildAudioMessageCommandEscapingReservedBytes() {
        Device device = new Device();
        device.setImei("123458800000015");
        device.setParserModel(ParserModel.TOUCH_4P.name());

        byte[] audioData = new byte[] { 0x7D, 0x5B, 0x2C, 'A' };
        String escapedAudio = new String(new byte[] {
                0x7D, 0x01,
                0x7D, 0x02,
                0x7D, 0x04,
                'A'
        }, StandardCharsets.ISO_8859_1);

        assertEquals("[3G*8800000015*000A*TK," + escapedAudio + "]",
                builder.buildAudioMessageCommand(device, audioData));
    }
}
