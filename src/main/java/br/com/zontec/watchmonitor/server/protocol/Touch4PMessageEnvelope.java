package br.com.zontec.watchmonitor.server.protocol;

public record Touch4PMessageEnvelope(
        String manufacturer,
        String deviceId,
        int declaredLength,
        String content
) {

    public static Touch4PMessageEnvelope parse(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new IllegalArgumentException("Mensagem Touch4P vazia ou nula.");
        }

        String trimmedMessage = rawMessage.trim();
        if (!trimmedMessage.startsWith("[") || !trimmedMessage.endsWith("]")) {
            throw new IllegalArgumentException("Mensagem Touch4P deve começar com '[' e terminar com ']'.");
        }

        String payload = trimmedMessage.substring(1, trimmedMessage.length() - 1);
        String[] parts = payload.split("\\*", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Envelope Touch4P inválido.");
        }

        int declaredLength = Integer.parseInt(parts[2], 16);
        return new Touch4PMessageEnvelope(parts[0], parts[1], declaredLength, parts[3]);
    }

    public String commandCode() {
        int separatorIndex = content.indexOf(',');
        return separatorIndex >= 0 ? content.substring(0, separatorIndex) : content;
    }

    public String[] contentParts() {
        return content.split(",");
    }

    public static String format(String manufacturer, String deviceId, String content) {
        return String.format("[%s*%s*%04X*%s]", manufacturer, deviceId, content.length(), content);
    }
}
