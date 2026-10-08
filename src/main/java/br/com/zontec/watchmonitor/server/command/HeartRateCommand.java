package br.com.zontec.watchmonitor.server.command;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

// Representa o comando IWAP49 - Frequência Cardíaca
public record HeartRateCommand(
        String imei,
        int heartRate,
        String rawMessage
) implements DeviceCommand {

    @Override
    public GPSWatchCommandType getType() {
        return GPSWatchCommandType.HEART_RATE;
    }

	public String imei() {
		return imei;
	}

	public int heartRate() {
		return heartRate;
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
		return rawMessage;
	}
}