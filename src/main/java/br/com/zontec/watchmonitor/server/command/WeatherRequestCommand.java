package br.com.zontec.watchmonitor.server.command;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

public record WeatherRequestCommand(
        String imei,
        String city,
        String rawMessage
) implements DeviceCommand {

    @Override
    public GPSWatchCommandType getType() {
        return GPSWatchCommandType.WEATHER_REQUEST;
    }
    
    public String getImei() {
		return imei;
	}

	@Override
	public String getRawMessage() {
		return rawMessage;
	}

	public String imei() {
		return imei;
	}

	public String city() {
		return city;
	}

	public String rawMessage() {
		return rawMessage;
	}
	
}