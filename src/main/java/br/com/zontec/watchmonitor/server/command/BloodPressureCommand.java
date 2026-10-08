package br.com.zontec.watchmonitor.server.command;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

public record BloodPressureCommand(
		String imei,
        int heartRate,
        int systolicPressure,
        int diastolicPressure,
		String rawMessage
) implements DeviceCommand{

	public GPSWatchCommandType getType() {
        return GPSWatchCommandType.BLOOD_PRESSURE;
    }

	public String imei() {
		return imei;
	}

	public int heartRate() {
		return heartRate;
	}

	public int systolicPressure() {
		return systolicPressure;
	}

	public int diastolicPressure() {
		return diastolicPressure;
	}

	public String rawMessage() {
		return rawMessage;
	}

	@Override
	public String getImei() {
		return imei;
	}

	@Override
	public String getRawMessage() {
		return rawMessage();
	}
	
	
}
