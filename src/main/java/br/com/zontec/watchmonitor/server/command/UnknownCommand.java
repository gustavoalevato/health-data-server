package br.com.zontec.watchmonitor.server.command;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

// Representa um comando que não pôde ser reconhecido ou parseado
public record UnknownCommand(
        String imei, // Pode ser null se não for possível extrair
        String rawMessage,
        String errorMessage
) implements DeviceCommand {

    @Override
    public GPSWatchCommandType getType() {
        return GPSWatchCommandType.UNKNOWN;
    }

	public String imei() {
		return imei;
	}

	public String rawMessage() {
		return rawMessage;
	}

	public String errorMessage() {
		return errorMessage;
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