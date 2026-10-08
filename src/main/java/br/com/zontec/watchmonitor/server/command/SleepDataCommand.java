package br.com.zontec.watchmonitor.server.command;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

public record SleepDataCommand(
        String imei,
        LocalDate date, 
        LocalTime startTime,
        LocalTime endTime,
        List<SleepEntryCommand> sleepEntries,
        int totalSleepDurationMinutes,
        String rawMessage
) implements DeviceCommand {

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
	
	public int totalSleepDurationMinutes() {
		return totalSleepDurationMinutes * 5;
	}
}