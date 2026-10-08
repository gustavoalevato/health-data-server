package br.com.zontec.watchmonitor.server.command;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

public interface DeviceCommand {
    GPSWatchCommandType getType();
    String getImei();
    String getRawMessage(); 
}