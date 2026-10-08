package br.com.zontec.watchmonitor.server.protocol.outbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.com.zontec.watchmonitor.server.api.dto.DeviceCommandAction;
import br.com.zontec.watchmonitor.server.api.dto.DeviceCommandRequest;
import br.com.zontec.watchmonitor.server.entities.Device;

class GpsWatchOutboundCommandBuilderTest {

    private final GpsWatchOutboundCommandBuilder builder = new GpsWatchOutboundCommandBuilder();

    @Test
    void shouldWrapRawPayloadAsGpsWatchCommand() {
        Device device = new Device();
        device.setImei("123456789012345");

        assertEquals("IWBP16#", builder.buildCommand(device, "BP16"));
    }

    @Test
    void shouldPreserveFullGpsWatchFrame() {
        Device device = new Device();
        device.setImei("123456789012345");

        assertEquals("IWBP16#", builder.buildCommand(device, "IWBP16#"));
    }

    @Test
    void shouldBuildLocateCommand() {
        Device device = new Device();
        device.setImei("123456789012345");

        assertEquals("IWBP16#", builder.buildLocateCommand(device));
    }

    @Test
    void shouldBuildTypedRawAndLocateCommands() {
        Device device = new Device();
        device.setImei("123456789012345");

        assertEquals("IWBP16#", builder.buildCommand(device,
                new DeviceCommandRequest(DeviceCommandAction.RAW, "BP16", null, null, null, null, null, null, null, false, false, null, null)));
        assertEquals("IWBP16#", builder.buildCommand(device,
                new DeviceCommandRequest(DeviceCommandAction.LOCATE, null, null, null, null, null, null, null, null, false, false, null, null)));
    }

    @Test
    void shouldRejectUnsupportedSemanticCommandForGpsWatch() {
        Device device = new Device();
        device.setImei("123456789012345");

        assertThrows(IllegalArgumentException.class, () -> builder.buildCommand(device,
                new DeviceCommandRequest(DeviceCommandAction.SET_UPLOAD_INTERVAL, null, 600, null, null, null, null, null, null, false, false, null, null)));
    }
}
