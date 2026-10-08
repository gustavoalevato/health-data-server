package br.com.zontec.watchmonitor.server.protocol.outbound;

import org.springframework.stereotype.Component;

import br.com.zontec.watchmonitor.server.api.dto.DeviceCommandRequest;
import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.enums.ParserModel;

@Component
public class GpsWatchOutboundCommandBuilder implements OutboundCommandBuilder {

    private static final String LOCATE_COMMAND = "BP16";

    @Override
    public String getParserModel() {
        return ParserModel.GPS_WATCH.name();
    }

    @Override
    public String buildCommand(Device device, String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Payload de comando não informado.");
        }

        String normalizedPayload = payload.trim();
        if (normalizedPayload.startsWith("IW") && normalizedPayload.endsWith("#")) {
            return normalizedPayload;
        }

        if (normalizedPayload.endsWith("#")) {
            return "IW" + normalizedPayload.substring(0, normalizedPayload.length() - 1) + "#";
        }

        return "IW" + normalizedPayload + "#";
    }

    @Override
    public String buildCommand(Device device, DeviceCommandRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requisição de comando não informada.");
        }

        return switch (request.normalizedAction()) {
            case RAW -> buildCommand(device, request.payload());
            case LOCATE -> buildLocateCommand(device);
            case FALLDOWN -> throw new IllegalArgumentException("Ação FALLDOWN ainda não possui helper semântico para GPS_WATCH.");
            case SET_UPLOAD_INTERVAL, CALL, MONITOR, CENTER, APN, HEALTH_MONITOR, HEALTH_AUTO_SET, BODY_TEMPERATURE, SET_GMT_BRASILIA ->
                    throw new IllegalArgumentException("Ação '" + request.normalizedAction()
                            + "' ainda não possui helper semântico para GPS_WATCH.");
        };
    }

    @Override
    public String buildLocateCommand(Device device) {
        return buildCommand(device, LOCATE_COMMAND);
    }
}
