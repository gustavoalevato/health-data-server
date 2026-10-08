package br.com.zontec.watchmonitor.server.command;

import java.math.BigDecimal;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

// Representa o comando IWAPHP - Dados de Saúde Abrangentes
// Formato: IWAPHP,IMEI,Heart rate,Systolic Blood Pressure,Diastolic Blood Pressure,SpO2,Blood sugar,Body temperature#
public record ComprehensiveHealthCommand(
        String imei,
        int heartRate,
        int systolicBloodPressure,  // Pressão Arterial Sistólica
        int diastolicBloodPressure, // Pressão Arterial Diastólica
        int spo2,                   // Saturação de Oxigênio no Sangue
        BigDecimal bloodSugar,      // Açúcar no Sangue (pode ser decimal)
        BigDecimal bodyTemperature, // Temperatura Corporal (pode ser decimal)
        String rawMessage
) implements DeviceCommand {

    @Override
    public GPSWatchCommandType getType() {
        return GPSWatchCommandType.COMPREHENSIVE_HEALTH;
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