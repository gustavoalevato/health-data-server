package br.com.zontec.watchmonitor.server.protocol;

import br.com.zontec.watchmonitor.server.command.DeviceCommand;

public interface CommandParser {

    String getParserModel();

    DeviceCommand parse(String rawMessage);
}
