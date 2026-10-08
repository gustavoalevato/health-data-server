package br.com.zontec.watchmonitor.server.command.types;

public enum GPSWatchCommandType {
	LOGIN("AP00"),
    LINK_KEEP("LK"),
    LOCATION("AP01"),
    CONFIG("CONFIG"),
    HEARTBEAT("AP03"),
    HEART_RATE("AP49"),
    BLOOD_PRESSURE("APHT"),
    COMPREHENSIVE_HEALTH("APHP"),
    BODY_TEMPERATURE("AP50"),     
    SLEEP_DATA("AP97"),            
    WEATHER_REQUEST("APWT"),       
    ECG_DATA("APHD"),              
    AUDIO_MESSAGE("AP07"),
    ALERT("AP10"),
    SPO2("oxygen"),
    UNKNOWN("UNKNOWN");

    private final String code;

    GPSWatchCommandType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static GPSWatchCommandType fromCode(String code) {
        for (GPSWatchCommandType type : GPSWatchCommandType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
