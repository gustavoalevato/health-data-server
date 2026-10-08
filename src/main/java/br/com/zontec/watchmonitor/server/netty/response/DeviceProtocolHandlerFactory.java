package br.com.zontec.watchmonitor.server.netty.response;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import br.com.zontec.watchmonitor.server.enums.ParserModel;

@Component
public class DeviceProtocolHandlerFactory {

    private final Map<String, DeviceProtocolHandler> handlersByModel = new ConcurrentHashMap<>();

    public DeviceProtocolHandlerFactory(List<DeviceProtocolHandler> handlers) {
        for (DeviceProtocolHandler handler : handlers) {
            handlersByModel.put(normalize(handler.getParserModel()), handler);
        }
    }

    public DeviceProtocolHandler getHandler(String parserModel) {
        DeviceProtocolHandler handler = handlersByModel.get(normalize(parserModel));
        if (handler != null) {
            return handler;
        }

        DeviceProtocolHandler defaultHandler = handlersByModel.get(ParserModel.GPS_WATCH.name());
        if (defaultHandler != null) {
            return defaultHandler;
        }

        return handlersByModel.values().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhum DeviceProtocolHandler registrado."));
    }

    private String normalize(String parserModel) {
        if (parserModel == null || parserModel.isBlank()) {
            return ParserModel.GPS_WATCH.name();
        }
        return parserModel.trim().toUpperCase();
    }
}
