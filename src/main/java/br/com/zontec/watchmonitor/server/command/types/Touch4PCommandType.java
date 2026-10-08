package br.com.zontec.watchmonitor.server.command.types;

public enum Touch4PCommandType {
	LOGIN("LK"),
    LINK_KEEP("LK"),
    LOCATION("UD"),
    BLIND_SPOT_LOCATION("UD2"),
    LOCATION_LTE("UD_LTE"),
    HEARTBEAT("LKHB"),
    CONFIG("CONFIG"),
    HEART_RATE("bphrt"),
    SPO2("oxygen"),
    BLOOD_PRESSURE("APHT"),
    COMPREHENSIVE_HEALTH("APHP"),
    BODY_TEMPERATURE("btemp2"),     
    SLEEP_DATA("AP97"),            
    WEATHER_REQUEST("APWT"),       
    ECG_DATA("APHD"),
    AUDIO_MESSAGE("TK"),
    ALERT("AL"),
    ALERT_LTE("AL_LTE"),
    DEVMESSAGE("DEVMESSAGE"),
    UNKNOWN("UNKNOWN");

    private final String code;

    Touch4PCommandType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Touch4PCommandType fromCode(String code) {
        for (Touch4PCommandType type : Touch4PCommandType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
