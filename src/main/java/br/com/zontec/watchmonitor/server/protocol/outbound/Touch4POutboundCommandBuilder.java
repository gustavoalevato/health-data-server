package br.com.zontec.watchmonitor.server.protocol.outbound;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.zontec.watchmonitor.server.api.dto.DeviceCommandRequest;
import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.enums.ParserModel;
import br.com.zontec.watchmonitor.server.protocol.Touch4PMessageEnvelope;
import br.com.zontec.watchmonitor.server.utils.Touch4PAudioMessageCodec;

@Component
public class Touch4POutboundCommandBuilder implements OutboundCommandBuilder {

    private static final String LOCATE_COMMAND = "CR";
    private static final int MIN_UPLOAD_INTERVAL = 60;
    private static final int MAX_UPLOAD_INTERVAL = 65535;

    private final String defaultManufacturer;

    public Touch4POutboundCommandBuilder(
            @Value("${touch4p.default-manufacturer:3G}") String defaultManufacturer) {
        this.defaultManufacturer = defaultManufacturer;
    }

    @Override
    public String getParserModel() {
        return ParserModel.TOUCH_4P.name();
    }

    @Override
    public String buildCommand(Device device, String payload) {
        if (device == null) {
            throw new IllegalArgumentException("Dispositivo não informado para montagem do comando Touch4P.");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Payload de comando não informado.");
        }

        String normalizedPayload = payload.trim();
        if (normalizedPayload.startsWith("[") && normalizedPayload.endsWith("]")) {
            return normalizedPayload;
        }

        return wrap(device, normalizedPayload);
    }

    @Override
    public String buildCommand(Device device, DeviceCommandRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requisição de comando não informada.");
        }

        return switch (request.normalizedAction()) {
            case RAW -> buildCommand(device, request.payload());
            case LOCATE -> buildLocateCommand(device);
            case FALLDOWN -> buildFalldownCommand(device, request.fallDownEnabled(), request.fallDownAlarmEnabled());
            case SET_UPLOAD_INTERVAL -> buildUploadCommand(device, request.intervalSeconds());
            case CALL -> buildCallCommand(device, request.phoneNumber());
            case MONITOR -> buildMonitorCommand(device, request.phoneNumber());
            case CENTER -> buildCenterCommand(device, request.centerNumber());
            case APN -> buildApnCommand(device, request.apnName(), request.apnUser(), request.apnPassword(),
                    request.mccmncCode());
            case HEALTH_MONITOR -> buildHealthMonitor(device, request.healthMonitorInterval());
            case HEALTH_AUTO_SET -> buildHealthAutoset(device, request.healthAutosetInterval());
            case BODY_TEMPERATURE -> buildBodyTemperatureCommand(device);
            case SET_GMT_BRASILIA -> buildSetGMTBrasilia(device);
        };
    }

    @Override
    public String buildLocateCommand(Device device) {
        return buildCommand(device, LOCATE_COMMAND);
    }

    public String buildHealthAutoset(Device device, Integer intervalMinutes) {
        if (intervalMinutes == null) {
            throw new IllegalArgumentException("O intervalo deve ser informado.");
        }

        return wrap(device, "HEALTHAUTOSET,1,1," + intervalMinutes);
    }

    public String buildHealthMonitor(Device device, Integer intervalSeconds) {
        if (intervalSeconds == null) {
            throw new IllegalArgumentException("O intervalo deve ser informado.");
        }
        if (intervalSeconds < MIN_UPLOAD_INTERVAL || intervalSeconds > MAX_UPLOAD_INTERVAL) {
            throw new IllegalArgumentException("O intervalo deve estar entre 60 e 65535 segundos.");
        }

        return wrap(device, "hrtstart," + intervalSeconds);
    }

    public String buildBodyTemperatureCommand(Device device) {
        return wrap(device, "bodytemp2");
    }

    public String buildSetGMTBrasilia(Device device) {
        return wrap(device, "LZ,3,-3");
    }

    public String buildUploadCommand(Device device, Integer intervalSeconds) {
        if (intervalSeconds == null) {
            throw new IllegalArgumentException("O intervalo de upload deve ser informado.");
        }
        if (intervalSeconds < MIN_UPLOAD_INTERVAL || intervalSeconds > MAX_UPLOAD_INTERVAL) {
            throw new IllegalArgumentException("O intervalo de upload deve estar entre 60 e 65535 segundos.");
        }

        return wrap(device, "UPLOAD," + intervalSeconds);
    }

    public String buildFalldownCommand(Device device, Boolean fallDownEnabled, Boolean fallDownAlarmEnabled) {
        return wrap(device, "FALLDOWN," + (fallDownEnabled ? "1" : "0") + "," + (fallDownAlarmEnabled ? "1" : "0"));
    }

    public String buildCallCommand(Device device, String phoneNumber) {
        return wrap(device, "CALL," + requireDigits(phoneNumber, "O telefone da chamada deve ser informado."));
    }

    public String buildMonitorCommand(Device device, String phoneNumber) {
        return wrap(device, "MONITOR," + requireDigits(phoneNumber, "O telefone do monitor deve ser informado."));
    }

    public String buildCenterCommand(Device device, String centerNumber) {
        return wrap(device, "CENTER," + requireDigits(centerNumber, "O número central deve ser informado."));
    }

    public String buildApnCommand(Device device, String apnName, String apnUser, String apnPassword, String mccmncCode) {
        if (apnName == null || apnName.isBlank()) {
            throw new IllegalArgumentException("O nome do APN deve ser informado.");
        }
        if (mccmncCode == null || mccmncCode.isBlank()) {
            throw new IllegalArgumentException("O código MCCMNC deve ser informado.");
        }

        String normalizedUser = apnUser == null ? "" : apnUser.trim();
        String normalizedPassword = apnPassword == null ? "" : apnPassword.trim();
        return wrap(device, String.format("APN,%s,%s,%s,%s",
                apnName.trim(), normalizedUser, normalizedPassword, mccmncCode.trim()));
    }

    public String buildAudioMessageCommand(Device device, byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("O arquivo de áudio deve ser informado.");
        }

        return wrap(device, "TK," + Touch4PAudioMessageCodec.encodeEscapedAudio(audioData));
    }

    private String wrap(Device device, String content) {
        return Touch4PMessageEnvelope.format(resolveManufacturer(), resolveDeviceId(device), content);
    }

    private String resolveManufacturer() {
        return defaultManufacturer == null || defaultManufacturer.isBlank()
                ? "3G"
                : defaultManufacturer.trim().toUpperCase();
    }

    private String resolveDeviceId(Device device) {
        String imei = device.getImei();
        if (imei == null || imei.isBlank()) {
            throw new IllegalArgumentException("Dispositivo sem IMEI para montagem do comando Touch4P.");
        }

        String digitsOnly = imei.trim();
        if (digitsOnly.length() < 10) {
            throw new IllegalArgumentException("IMEI inválido para montagem do deviceId Touch4P: " + imei);
        }

        return digitsOnly.substring(digitsOnly.length() - 10);
    }

    private String requireDigits(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }

        return value.trim();
    }
}
