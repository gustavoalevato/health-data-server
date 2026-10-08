package br.com.zontec.watchmonitor.server.command;

import java.math.BigDecimal;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;


public record BodyTemperatureCommand(
        String imei,
        BigDecimal temperature,
        String rawMessage
) implements DeviceCommand {

    @Override
    public GPSWatchCommandType getType() {
        return GPSWatchCommandType.BODY_TEMPERATURE;
    }
    
    public String getImei() {
		return imei;
	}

	@Override
	public String getRawMessage() {
		return rawMessage;
	}
}