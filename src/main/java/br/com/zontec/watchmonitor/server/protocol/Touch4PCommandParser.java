package br.com.zontec.watchmonitor.server.protocol;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.zontec.watchmonitor.server.command.AlertCommand;
import br.com.zontec.watchmonitor.server.command.AudioMessageCommand;
import br.com.zontec.watchmonitor.server.command.AudioReceivedMessageCommand;
import br.com.zontec.watchmonitor.server.command.BloodPressureCommand;
import br.com.zontec.watchmonitor.server.command.BodyTemperatureCommand;
import br.com.zontec.watchmonitor.server.command.ComprehensiveHealthCommand;
import br.com.zontec.watchmonitor.server.command.ConfigCommand;
import br.com.zontec.watchmonitor.server.command.DeviceCommand;
import br.com.zontec.watchmonitor.server.command.EcgDataCommand;
import br.com.zontec.watchmonitor.server.command.HeartRateCommand;
import br.com.zontec.watchmonitor.server.command.LinkKeepCommand;
import br.com.zontec.watchmonitor.server.command.LocationCommand;
import br.com.zontec.watchmonitor.server.command.SleepDataCommand;
import br.com.zontec.watchmonitor.server.command.SleepEntryCommand;
import br.com.zontec.watchmonitor.server.command.Spo2Command;
import br.com.zontec.watchmonitor.server.command.UnknownCommand;
import br.com.zontec.watchmonitor.server.command.WeatherRequestCommand;
import br.com.zontec.watchmonitor.server.command.types.Touch4PCommandType;
import br.com.zontec.watchmonitor.server.enums.ParserModel;
import br.com.zontec.watchmonitor.server.utils.Touch4PAudioMessageCodec;

@Component
public class Touch4PCommandParser implements CommandParser {

    private static final Logger log = LoggerFactory.getLogger(Touch4PCommandParser.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter TIME_FORMATTER_AP97 = DateTimeFormatter.ofPattern("HHmm");

    @Override
    public String getParserModel() {
        return ParserModel.TOUCH_4P.name();
    }

    @Override
    public DeviceCommand parse(String rawMessage) {
        String identifier = null;

        try {
            Touch4PMessageEnvelope envelope = Touch4PMessageEnvelope.parse(rawMessage);
            identifier = envelope.deviceId();
            String[] parts = envelope.contentParts();
            Touch4PCommandType commandType = Touch4PCommandType.fromCode(envelope.commandCode());

            return switch (commandType) {
                case LOGIN, LINK_KEEP -> parseLinkKeep(identifier, parts, rawMessage);
                case LOCATION, LOCATION_LTE,BLIND_SPOT_LOCATION -> parseLocation(identifier, parts, rawMessage);
                case ALERT, ALERT_LTE -> parseAlert(envelope, parts, rawMessage);
                case CONFIG -> parseConfig(envelope, rawMessage);
                case HEART_RATE -> parseHeartRate(identifier, parts, rawMessage);
                case BLOOD_PRESSURE -> parseBloodPressure(identifier, parts, rawMessage);
                case COMPREHENSIVE_HEALTH -> parseComprehensiveHealth(identifier, parts, rawMessage);
                case BODY_TEMPERATURE -> parseBodyTemperature(identifier, parts, rawMessage);
                case SLEEP_DATA -> parseSleepData(identifier, parts, rawMessage);
                case WEATHER_REQUEST -> parseWeatherRequest(identifier, parts, rawMessage);
                case ECG_DATA -> parseEcgData(identifier, parts, rawMessage);
                case SPO2 -> parseOxygen(identifier, parts, rawMessage);
                case AUDIO_MESSAGE -> parseAudioMessage(envelope, rawMessage);
                default -> new UnknownCommand(identifier, rawMessage,
                        "Comando Touch4P '" + envelope.commandCode() + "' reconhecido, mas não implementado.");
            };
        } catch (NumberFormatException e) {
            log.error("Erro de formato numérico ao parsear comando Touch4P: {}. Mensagem: {}", e.getMessage(), rawMessage, e);
            return new UnknownCommand(identifier, rawMessage, "Erro de formato numérico: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Erro de validação ao parsear comando Touch4P: {}. Mensagem: {}", e.getMessage(), rawMessage, e);
            return new UnknownCommand(identifier, rawMessage, "Erro de validação: " + e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao parsear comando Touch4P: {}. Mensagem: {}", e.getMessage(), rawMessage, e);
            return new UnknownCommand(identifier, rawMessage, "Erro inesperado: " + e.getMessage());
        }
    }

    private DeviceCommand parseLinkKeep(String identifier, String[] parts, String rawMessage) {
        int steps = parts.length > 1 ? parseInteger(parts[1]) : 0;
        int rollsFrequency = parts.length > 2 ? parseInteger(parts[2]) : 0;
        int battery = parts.length > 3 ? parseInteger(parts[3]) : 0;
        return new LinkKeepCommand(identifier, battery, 0, 0, steps, 0, rollsFrequency, null, rawMessage);
    }

    private DeviceCommand parseLocation(String identifier, String[] parts, String rawMessage) {
        ensureMinimumParts(parts, 17, "UD/UD2/UD_LTE");

        BigDecimal latitude = applyHemisphere(new BigDecimal(parts[4]), parts[5], true);
        BigDecimal longitude = applyHemisphere(new BigDecimal(parts[6]), parts[7], false);
        BigDecimal speed = new BigDecimal(parts[8]);
        String direction = parts[9];
        int gsm = parseInteger(parts[12]);
        int battery = parseInteger(parts[13]);

        return new LocationCommand(identifier, latitude, longitude, speed, direction, battery, gsm, rawMessage);
    }

    private DeviceCommand parseAlert(Touch4PMessageEnvelope envelope, String[] parts, String rawMessage) {
        ensureMinimumParts(parts, 17, "AL/AL_LTE");

        BigDecimal latitude = applyHemisphere(new BigDecimal(parts[4]), parts[5], true);
        BigDecimal longitude = applyHemisphere(new BigDecimal(parts[6]), parts[7], false);
        BigDecimal speed = new BigDecimal(parts[8]);
        String direction = parts[9];
        int gsm = parseInteger(parts[12]);
        int battery = parseInteger(parts[13]);
        String statusBits = parts[16];

        return new AlertCommand(envelope.deviceId(), envelope.manufacturer(), envelope.deviceId(), latitude, longitude,
                speed, direction, battery, gsm, statusBits, Boolean.TRUE, rawMessage);
    }

    private DeviceCommand parseConfig(Touch4PMessageEnvelope envelope, String rawMessage) {
        String payload = envelope.content().contains(",")
                ? envelope.content().substring(envelope.content().indexOf(',') + 1)
                : "";
        return new ConfigCommand(envelope.deviceId(), envelope.manufacturer(), envelope.deviceId(), payload, rawMessage);
    }

    private DeviceCommand parseHeartRate(String identifier, String[] parts, String rawMessage) {
        ensureMinimumParts(parts, 2, Touch4PCommandType.HEART_RATE.getCode());
        return new BloodPressureCommand(identifier, Integer.parseInt(parts[3]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), rawMessage);
    }


    private DeviceCommand parseOxygen(String identifier, String[] parts, String rawMessage) {
        ensureMinimumParts(parts, 1,  Touch4PCommandType.SPO2.getCode());
        return new Spo2Command(identifier, parseInteger(parts[1]), parseInteger(parts[2]), rawMessage);
    }

    private DeviceCommand parseBloodPressure(String identifier, String[] parts, String rawMessage) {
        ensureMinimumParts(parts, 4, "APHT");
        return new BloodPressureCommand(identifier, parseInteger(parts[1]), parseInteger(parts[2]), parseInteger(parts[3]), rawMessage);
    }

    private DeviceCommand parseComprehensiveHealth(String identifier, String[] parts, String rawMessage) {
        ensureMinimumParts(parts, 7, "APHP");
        return new ComprehensiveHealthCommand(identifier, parseInteger(parts[1]), parseInteger(parts[2]),
                parseInteger(parts[3]), parseInteger(parts[4]), new BigDecimal(parts[5]), new BigDecimal(parts[6]), rawMessage);
    }

    private DeviceCommand parseBodyTemperature(String identifier, String[] parts, String rawMessage) {
        ensureMinimumParts(parts, 2, Touch4PCommandType.BODY_TEMPERATURE.getCode());
        return new BodyTemperatureCommand(identifier, new BigDecimal(parts[2]), rawMessage);
    }

    private DeviceCommand parseSleepData(String identifier, String[] parts, String rawMessage) {
        ensureMinimumParts(parts, 4, "AP97");

        LocalDate date = LocalDate.now();
        String[] period = parts[1].split("@");
        if (period.length != 2) {
            throw new IllegalArgumentException("Período de sono inválido para AP97.");
        }

        LocalTime startTime = LocalTime.parse(period[0], TIME_FORMATTER_AP97);
        LocalTime endTime = LocalTime.parse(period[1], TIME_FORMATTER_AP97);
        int totalSleepDuration = parseInteger(parts[2]);

        List<SleepEntryCommand> sleepEntries = new ArrayList<>();
        for (int i = 4; i + 1 < parts.length; i += 2) {
            sleepEntries.add(new SleepEntryCommand(identifier, parseInteger(parts[i]), parseInteger(parts[i + 1]), rawMessage));
        }

        return new SleepDataCommand(identifier, date, startTime, endTime, sleepEntries, totalSleepDuration, rawMessage);
    }

    private DeviceCommand parseWeatherRequest(String identifier, String[] parts, String rawMessage) {
        String city = parts.length > 1 ? parts[1] : "";
        return new WeatherRequestCommand(identifier, city, rawMessage);
    }

    private DeviceCommand parseEcgData(String identifier, String[] parts, String rawMessage) {
        ensureMinimumParts(parts, 10, "APHD");

        LocalDateTime timestamp = LocalDateTime.parse(parts[1], DATE_TIME_FORMATTER);
        int totalPackets = parseInteger(parts[2]);
        int currentPacket = parseInteger(parts[3]);
        BigDecimal gain = new BigDecimal(parts[4]);
        BigDecimal zeroVoltage = new BigDecimal(parts[5]);
        int leadType = parseInteger(parts[6]);
        int samplingRate = parseInteger(parts[7]);

        List<String> ecgDataPoints = new ArrayList<>();
        for (int i = 9; i < parts.length; i++) {
            ecgDataPoints.add(parts[i]);
        }

        return new EcgDataCommand(identifier, timestamp, totalPackets, currentPacket, gain, zeroVoltage, leadType,
                samplingRate, ecgDataPoints, rawMessage);
    }

    private DeviceCommand parseAudioMessage(Touch4PMessageEnvelope envelope, String rawMessage) {
        int separatorIndex = envelope.content().indexOf(',');
        if (separatorIndex < 0) {
            log.warn("Comando de áudio TK recebido com payload vazio, indicando que o dispositivo não recebeu dados de áudio. Mensagem original: {}", rawMessage);
            return new AudioReceivedMessageCommand(envelope.deviceId(), envelope.manufacturer(), envelope.deviceId(), 0, rawMessage);
        }

        String audioPayload = envelope.content().substring(separatorIndex + 1);

        if (audioPayload.equals("0")){
            log.warn("Comando de áudio TK recebido com payload '0', indicando que o dispositivo não recebeu dados de áudio. Mensagem original: {}", rawMessage);
            return new AudioReceivedMessageCommand(envelope.deviceId(), envelope.manufacturer(), envelope.deviceId(), 0, rawMessage);
        }else if (audioPayload.equals("1")) {
            log.warn("Comando de áudio TK recebido com payload '1', indicando que o dispositivo recebeu um comando de áudio. Mensagem original: {}", rawMessage);
            return new AudioReceivedMessageCommand(envelope.deviceId(), envelope.manufacturer(), envelope.deviceId(), 1, rawMessage);
        }

        byte[] audioData = Touch4PAudioMessageCodec.decodeEscapedAudio(audioPayload);
        return new AudioMessageCommand(envelope.deviceId(), envelope.manufacturer(), envelope.deviceId(), audioData, rawMessage);
    }

    private void ensureMinimumParts(String[] parts, int minimumLength, String commandName) {
        if (parts.length < minimumLength) {
            throw new IllegalArgumentException("Número insuficiente de campos para " + commandName + ".");
        }
    }

    private int parseInteger(String value) {
        return Integer.parseInt(value.trim());
    }

    private BigDecimal applyHemisphere(BigDecimal value, String hemisphere, boolean northEastPositive) {
        // Deixa o valor positivo de qualquer forma, para validar o hemisferio
        value = value.abs();

        if (hemisphere == null || hemisphere.isBlank()) {
            return value;
        }

        String normalizedHemisphere = hemisphere.trim().toUpperCase();
        boolean positive = northEastPositive
                ? "N".equals(normalizedHemisphere)
                : "E".equals(normalizedHemisphere);

        return positive ? value : value.negate();
    }
}
