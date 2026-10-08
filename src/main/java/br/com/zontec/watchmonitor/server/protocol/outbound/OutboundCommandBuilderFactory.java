package br.com.zontec.watchmonitor.server.protocol.outbound;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import br.com.zontec.watchmonitor.server.enums.ParserModel;

@Component
public class OutboundCommandBuilderFactory {

    private final Map<String, OutboundCommandBuilder> buildersByModel = new ConcurrentHashMap<>();

    public OutboundCommandBuilderFactory(List<OutboundCommandBuilder> builders) {
        for (OutboundCommandBuilder builder : builders) {
            buildersByModel.put(normalize(builder.getParserModel()), builder);
        }
    }

    public OutboundCommandBuilder getBuilder(String parserModel) {
        OutboundCommandBuilder builder = buildersByModel.get(normalize(parserModel));
        if (builder != null) {
            return builder;
        }

        OutboundCommandBuilder defaultBuilder = buildersByModel.get(ParserModel.GPS_WATCH.name());
        if (defaultBuilder != null) {
            return defaultBuilder;
        }

        return buildersByModel.values().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhum OutboundCommandBuilder registrado."));
    }

    private String normalize(String parserModel) {
        if (parserModel == null || parserModel.isBlank()) {
            return ParserModel.GPS_WATCH.name();
        }

        return parserModel.trim().toUpperCase();
    }
}
