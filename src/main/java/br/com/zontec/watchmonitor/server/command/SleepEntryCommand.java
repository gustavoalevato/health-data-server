package br.com.zontec.watchmonitor.server.command;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

// Representa uma única entrada de sono (tipo e duração)
public record SleepEntryCommand(
		String imei,
        int sleepType,    // 0: Awake, 1: Light Sleep, 2: Deep Sleep, 3: Not Wearing
        int durationMinutes,
        String rawMessage
) implements DeviceCommand{

	@Override
    public GPSWatchCommandType getType() {
        return GPSWatchCommandType.SLEEP_DATA;
    }
    
    public String getImei() {
		return imei;
	}

	@Override
	public String getRawMessage() {
		return rawMessage;
	}

	public int sleepType() {
		return sleepType;
	}

	public int durationMinutes() {
		return durationMinutes;
	}
	
}

// Representa o comando IWAP97 - Dados de Sono
// Formato: IWAP97,imei,YYYYMMDD,HHMMSS,HHMMSS,SleepType1,SleepDuration1,SleepType2,SleepDuration2,......TotalSleepDuration#
