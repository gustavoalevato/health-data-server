package br.com.zontec.watchmonitor.server.netty.response;

import java.time.LocalDateTime;
import java.util.Random;

import br.com.zontec.watchmonitor.server.command.DeviceCommand;

public interface DeviceProtocolHandler {

    String getParserModel();

    String loginResponse(LocalDateTime now, int timezone);

    String loginResponse(String fabric, String deviceId);

    String acknowledge(DeviceCommand command);

    String weatherResponse(LocalDateTime now, Random random);

    String ecgAnalysisResponse(String imei, Random random);
}
