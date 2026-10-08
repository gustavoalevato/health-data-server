package br.com.zontec.watchmonitor.server.protocol.outbound;

import br.com.zontec.watchmonitor.server.api.dto.DeviceCommandRequest;
import br.com.zontec.watchmonitor.server.entities.Device;

public interface OutboundCommandBuilder {

    String getParserModel();

    String buildCommand(Device device, String payload);

    String buildCommand(Device device, DeviceCommandRequest request);

    String buildLocateCommand(Device device);
}
