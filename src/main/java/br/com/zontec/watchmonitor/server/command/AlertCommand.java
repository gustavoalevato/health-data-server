package br.com.zontec.watchmonitor.server.command;

import java.math.BigDecimal;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

public record AlertCommand(
        String imei,
        String manufacturer,
        String deviceId,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal speed,
        String direction,
        int batteryPercentage,
        int gsmStrength,
        String statusBits,
        Boolean isSOSAlert,
        String rawMessage
) implements DeviceCommand {

    @Override
    public GPSWatchCommandType getType() {
        return GPSWatchCommandType.ALERT;
    }

    @Override
    public String getImei() {
        return imei;
    }

    @Override
    public String getRawMessage() {
        return rawMessage;
    }

    public LocationCommand toLocationCommand() {
        return new LocationCommand(imei, latitude, longitude, speed, direction, batteryPercentage, gsmStrength, rawMessage);
    }

    public boolean isFallDownAlarm() {
        return hasBitSet(21);
    }

    public Boolean isSOSAlert() {
        return isSOSAlert != null && isSOSAlert;
    }

    public boolean hasBitSet(int bitPosition) {
        if (statusBits == null || statusBits.isBlank() || bitPosition < 0) {
            return false;
        }

        try {
            long statusValue = Long.parseUnsignedLong(statusBits.trim(), 16);
            return (statusValue & (1L << bitPosition)) != 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
