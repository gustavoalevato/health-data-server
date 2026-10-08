package br.com.zontec.watchmonitor.server.protocol;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import br.com.zontec.watchmonitor.server.enums.ParserModel;

@Component
public class CommandParserFactory {

    private final Map<String, CommandParser> parsersByModel = new ConcurrentHashMap<>();

    public CommandParserFactory(List<CommandParser> parsers) {
        for (CommandParser parser : parsers) {
            parsersByModel.put(normalize(parser.getParserModel()), parser);
        }
    }

    public CommandParser getParser(String parserModel) {
        CommandParser parser = parsersByModel.get(normalize(parserModel));
        if (parser != null) {
            return parser;
        }

        CommandParser defaultParser = parsersByModel.get(ParserModel.GPS_WATCH.name());
        if (defaultParser != null) {
            return defaultParser;
        }

        return parsersByModel.values().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhum parser registrado no CommandParserFactory."));
    }

    private String normalize(String parserModel) {
        if (parserModel == null || parserModel.isBlank()) {
            return ParserModel.GPS_WATCH.name();
        }

        return parserModel.trim().toUpperCase();
    }
}
