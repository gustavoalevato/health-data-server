package br.com.zontec.watchmonitor.server.netty.response;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.stereotype.Component;

import br.com.zontec.watchmonitor.server.command.ConfigCommand;
import br.com.zontec.watchmonitor.server.command.DeviceCommand;
import br.com.zontec.watchmonitor.server.command.Spo2Command;
import br.com.zontec.watchmonitor.server.command.types.Touch4PCommandType;
import br.com.zontec.watchmonitor.server.enums.ParserModel;
import br.com.zontec.watchmonitor.server.protocol.Touch4PMessageEnvelope;

@Component
public class Touch4PDeviceProtocolHandler extends GpsWatchDeviceProtocolHandler {

    @Override
    public String getParserModel() {
        return ParserModel.TOUCH_4P.name();
    }

    public String loginResponse(String fabric, String deviceId) {
        return Touch4PMessageEnvelope.format(fabric, deviceId, Touch4PCommandType.LOGIN.getCode());
    }

    @Override
    public String acknowledge(DeviceCommand command) {
        if (command == null || command.getRawMessage() == null) {
            return "";
        }

        Touch4PMessageEnvelope envelope = Touch4PMessageEnvelope.parse(command.getRawMessage());

        if (command instanceof ConfigCommand) {
            return Touch4PMessageEnvelope.format(envelope.manufacturer(), envelope.deviceId(), "CONFIG,1");
        }

        if (command instanceof Spo2Command) {
            return Touch4PMessageEnvelope.format(envelope.manufacturer(), envelope.deviceId(), String.format("%s,%d", "oxygen", ((Spo2Command) command).oxygenRate()));
        }

        if (Touch4PCommandType.AUDIO_MESSAGE.getCode().equals(envelope.commandCode())) {
            return Touch4PMessageEnvelope.format(envelope.manufacturer(), envelope.deviceId(), "TK,1");
        }

        String commandCode = envelope.commandCode();
        if (Touch4PCommandType.LOCATION.getCode().equals(commandCode)
                || Touch4PCommandType.BLIND_SPOT_LOCATION.getCode().equals(commandCode)) {
            return "";
        }

        return Touch4PMessageEnvelope.format(envelope.manufacturer(), envelope.deviceId(), commandCode);
    }

    @Override
    public String weatherResponse(LocalDateTime now, Random random) {
        return "";
    }

    @Override
    public String ecgAnalysisResponse(String imei, Random random) {
        return "";
    }
}
