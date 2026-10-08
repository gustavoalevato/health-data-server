package br.com.zontec.watchmonitor.server.netty.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import org.springframework.stereotype.Component;

import br.com.zontec.watchmonitor.server.command.DeviceCommand;
import br.com.zontec.watchmonitor.server.enums.ParserModel;

@Component
public class GpsWatchDeviceProtocolHandler implements DeviceProtocolHandler {

    private static final DateTimeFormatter DATE_TIME_FORMATTER_AP00_BP00 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DATE_TIME_FORMATTER_BPWT = DateTimeFormatter.ofPattern("yyMMddHHMMSS");

    @Override
    public String getParserModel() {
        return ParserModel.GPS_WATCH.name();
    }

    @Override
    public String loginResponse(LocalDateTime now, int timezone) {
        return String.format("IWBP00,%s,%d#", now.format(DATE_TIME_FORMATTER_AP00_BP00), timezone);
    }

    @Override
    public String loginResponse(String fabric, String deviceId) {
        return "";
    }

    @Override
    public String acknowledge(DeviceCommand command) {
        if (command == null || command.getType() == null) {
            return "";
        }

        return switch (command.getType()) {
            case LOCATION -> "IWBP01#";
            case HEARTBEAT -> "IWBP03#";
            case HEART_RATE -> "IWBP49#";
            case BLOOD_PRESSURE -> "IWBPHT#";
            case COMPREHENSIVE_HEALTH -> "IWBPHP#";
            case BODY_TEMPERATURE -> "IWBP50#";
            case SLEEP_DATA -> "IWBP97#";
            case ALERT -> "IWBP10#";
            case CONFIG -> "";
            default -> "";
        };
    }

    @Override
    public String weatherResponse(LocalDateTime now, Random random) {
        int temp = 20 + random.nextInt(10);
        int humidity = 50 + random.nextInt(30);
        String weather = "Sunny";
        int uv = random.nextInt(10);
        int windSpeed = random.nextInt(15);
        String windDirection = "NW";
        int weatherCode = 100;
        int pm25 = 10 + random.nextInt(50);
        int aqi = 20 + random.nextInt(80);

        return String.format("IWBPWT,%s,%d,%d,%s,%d,%d,%s,%d,%d,%d#",
                now.format(DATE_TIME_FORMATTER_BPWT), temp, humidity, weather, uv, windSpeed,
                windDirection, weatherCode, pm25, aqi);
    }

    @Override
    public String ecgAnalysisResponse(String imei, Random random) {
        String aiAnalysisStatus = "OK";
        int hr = 60 + random.nextInt(40);
        int qrs = random.nextInt(100);
        int pr = random.nextInt(150);
        int qt = random.nextInt(400);
        int qtc = random.nextInt(450);
        String afib = random.nextBoolean() ? "Yes" : "No";

        return String.format("IWBPXL,%s,%s,%d,%d,%d,%d,%d,%s#",
                imei, aiAnalysisStatus, hr, qrs, pr, qt, qtc, afib);
    }

}
