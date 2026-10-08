package br.com.zontec.watchmonitor.server.command;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

// Representa o comando IWAP03 - Heartbeat
public record HeartbeatCommand(
        String imei,
        int batteryPercentage,
        int gsmStrength,
        int numSatellites,
        int steps,
        int spaceLeft,
        int rollsFrequency,
        String status, // Ex: "0" para normal, "1" para fortificação
        String rawMessage
) implements DeviceCommand {

    @Override
    public GPSWatchCommandType getType() {
        return GPSWatchCommandType.HEARTBEAT;
    }

	public String imei() {
		return imei;
	}

	public int batteryPercentage() {
		return batteryPercentage;
	}

	public int gsmStrength() {
		return gsmStrength;
	}

	public int numSatellites() {
		return numSatellites;
	}

	public int steps() {
		return steps;
	}

	public String status() {
		return status;
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

	public int spaceLeft() {
		return spaceLeft;
	}

	public int rollsFrequency() {
		return rollsFrequency;
	}
}