package br.com.zontec.watchmonitor.server.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

public record EcgDataCommand(
        String imei,
        LocalDateTime timestamp,
        int totalPackets,   // Número total de pacotes para este ECG
        int currentPacket,  // Número do pacote atual
        BigDecimal gain,           // Ganho (ex: 200, 400)
        BigDecimal zeroVoltage,    // Voltagem zero
        int leadType,       // Tipo de derivação (ex: 0: 1-lead)
        int samplingRate,   // Taxa de amostragem
        List<String> ecgDataPoints, // Os dados brutos do ECG
        String rawMessage
) implements DeviceCommand {

    @Override
    public GPSWatchCommandType getType() {
        return GPSWatchCommandType.ECG_DATA;
    }
    
    public String getImei() {
		return imei;
	}

	@Override
	public String getRawMessage() {
		return rawMessage;
	}
	
	public String getEcgDataPointsJson() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            
            return objectMapper.writeValueAsString(ecgDataPoints);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter para JSON", e);
        }
    }
}