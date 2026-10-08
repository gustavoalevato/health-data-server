package br.com.zontec.watchmonitor.server.command;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

public record ConfigCommand(
        String imei,
        String manufacturer,
        String deviceId,
        String payload,
        String rawMessage
) implements DeviceCommand {

    @Override
    public GPSWatchCommandType getType() {
        return GPSWatchCommandType.CONFIG;
    }

    @Override
    public String getImei() {
        return imei;
    }

    @Override
    public String getRawMessage() {
        return rawMessage;
    }
}
