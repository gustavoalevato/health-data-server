package br.com.zontec.watchmonitor.server.command;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

public record Spo2Command(
		String imei,
		int type,
        int spo2,
		String rawMessage
) implements DeviceCommand{

	public GPSWatchCommandType getType() {
        return GPSWatchCommandType.SPO2;
    }

	public String imei() {
		return imei;
	}

	public int type() {
		return type;
	}

	public int spo2() {
		return spo2;
	}

	public int oxygenRate(){
		if(spo2 >= 90) {
			return 1; // Normal
		} else if (spo2 >= 70) {
			return 2; // Baixo
		} else {
			return 0; // Crítico
		}
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
