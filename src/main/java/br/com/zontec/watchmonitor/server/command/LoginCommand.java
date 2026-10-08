package br.com.zontec.watchmonitor.server.command;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

public record LoginCommand(
        String imei,
        String rawMessage // A mensagem original para referência
) implements DeviceCommand {

    @Override
    public GPSWatchCommandType getType() {
        return GPSWatchCommandType.LOGIN;
    }

	@Override
	public String getImei() {
		return this.imei;
	}

	@Override
	public String getRawMessage() {
		return this.rawMessage;
	}
}