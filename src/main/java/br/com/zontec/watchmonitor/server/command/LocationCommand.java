package br.com.zontec.watchmonitor.server.command;

import java.math.BigDecimal;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

// Representa o comando IWAP01 - Localização
// Simplificado, o AP01 tem muito mais campos!
public record LocationCommand(
        String imei,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal speed, // Velocidade (no protocolo é string)
        String direction, // Direção (no protocolo é string)
        int batteryPercentage,
        int gsmStrength,
        String rawMessage
) implements DeviceCommand {

	@Override
    public GPSWatchCommandType getType() {
        return GPSWatchCommandType.LOCATION;
    }
    
    @Override
	public String getImei() {
		return this.imei;
	}

	@Override
	public String getRawMessage() {
		return this.rawMessage;
	}

	public String imei() {
		return imei;
	}

	public BigDecimal latitude() {
		return latitude;
	}

	public BigDecimal longitude() {
		return longitude;
	}

	public BigDecimal speed() {
		return speed;
	}

	public String direction() {
		return direction;
	}

	public int batteryPercentage() {
		return batteryPercentage;
	}

	public int gsmStrength() {
		return gsmStrength;
	}

	public String rawMessage() {
		return rawMessage;
	}
}