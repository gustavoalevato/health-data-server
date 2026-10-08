package br.com.zontec.watchmonitor.server.netty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.zontec.watchmonitor.server.netty.response.DeviceProtocolHandlerFactory;
import br.com.zontec.watchmonitor.server.protocol.CommandParserFactory;
import br.com.zontec.watchmonitor.server.service.DeviceService;

@Component
public class GpsWatchProtocolHandlerFactory {

    @Autowired
    private DeviceService deviceService; 

    @Autowired
    private CommandParserFactory commandParserFactory;

    @Autowired
    private DeviceProtocolHandlerFactory deviceProtocolHandlerFactory;

    public GpsWatchProtocolHandler createHandler() {
        return new GpsWatchProtocolHandler(deviceService, commandParserFactory, deviceProtocolHandlerFactory);
    }
}
