package br.com.zontec.watchmonitor.server.command;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

public record AudioMessageCommand(
        String imei,
        String manufacturer,
        String deviceId,
        byte[] audioData,
        String rawMessage
) implements DeviceCommand {

    @Override
    public GPSWatchCommandType getType() {
        return GPSWatchCommandType.AUDIO_MESSAGE;
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
