package br.com.zontec.watchmonitor.server.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.zontec.watchmonitor.server.entities.BloodPressure;
import br.com.zontec.watchmonitor.server.entities.BodyTemperature;
import br.com.zontec.watchmonitor.server.entities.ComprehensiveHealth;
import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.entities.EcgData;
import br.com.zontec.watchmonitor.server.entities.FallDownAlert;
import br.com.zontec.watchmonitor.server.entities.HeartRate;
import br.com.zontec.watchmonitor.server.entities.Heartbeat;
import br.com.zontec.watchmonitor.server.entities.Location;
import br.com.zontec.watchmonitor.server.entities.Oxygen;
import br.com.zontec.watchmonitor.server.entities.SleepEntry;
import br.com.zontec.watchmonitor.server.entities.SleepSession;
import br.com.zontec.watchmonitor.server.repositories.BloodPressureRepository;
import br.com.zontec.watchmonitor.server.repositories.BodyTemperatureRepository;
import br.com.zontec.watchmonitor.server.repositories.ComprehensiveHealthRepository;
import br.com.zontec.watchmonitor.server.repositories.DeviceRepository;
import br.com.zontec.watchmonitor.server.repositories.EcgDataRepository;
import br.com.zontec.watchmonitor.server.repositories.FallDownAlertRepository;
import br.com.zontec.watchmonitor.server.repositories.HeartRateRepository;
import br.com.zontec.watchmonitor.server.repositories.HeartbeatRepository;
import br.com.zontec.watchmonitor.server.repositories.LocationRepository;
import br.com.zontec.watchmonitor.server.repositories.OxygenRepository;
import br.com.zontec.watchmonitor.server.repositories.SleepEntryRepository;
import br.com.zontec.watchmonitor.server.repositories.SleepSessionRepository;

@Service
public class HealthService {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ComprehensiveHealthRepository comprehensiveHealthRepository;

    @Autowired
    private HeartRateRepository heartRateRepository;

    @Autowired
    private BodyTemperatureRepository bodyTemperatureRepository;

    @Autowired
    private BloodPressureRepository bloodPressureRepository;

    @Autowired
    private SleepSessionRepository sleepSessionRepository;

    @Autowired
    private EcgDataRepository ecgDataRepository;

    @Autowired
    private HeartbeatRepository heartbeatRepository;

    @Autowired
    private OxygenRepository oxygenRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private FallDownAlertRepository fallDownAlertRepository;

    @Autowired
    private SleepEntryRepository sleepEntryRepository;


    public List<ComprehensiveHealth> getComprehensiveHealth(String imei, Long since) {

        Device device = deviceRepository.findByImei(imei).orElseThrow(() -> new RuntimeException("Device not found: " + imei));

        return comprehensiveHealthRepository.findRecentHealthData(device, LocalDateTime.now().minusMinutes(since));
    }

    public List<HeartRate> getHeartRates(String imei, Long since) {

        Device device = deviceRepository.findByImei(imei).orElseThrow(() -> new RuntimeException("Device not found: " + imei));

        return heartRateRepository.findRecentHeartRates(device, LocalDateTime.now().minusMinutes(since));
    }

    public List<BodyTemperature> getBodyTemperatures(String imei, Long since) {

        Device device = deviceRepository.findByImei(imei).orElseThrow(() -> new RuntimeException("Device not found: " + imei));

        return bodyTemperatureRepository.findRecentTemperatures(device, LocalDateTime.now().minusMinutes(since));
    }

    public List<BloodPressure> getBloodPressures(String imei, Long since) {

        Device device = deviceRepository.findByImei(imei).orElseThrow(() -> new RuntimeException("Device not found: " + imei));

        return bloodPressureRepository.findRecentBloodPressures(device, LocalDateTime.now().minusMinutes(since));
    }


    public List<SleepSession> getSleepSessions(String imei, Long since) {

        Device device = deviceRepository.findByImei(imei).orElseThrow(() -> new RuntimeException("Device not found: " + imei));

        return sleepSessionRepository.findRecentSleepSessions(device, LocalDateTime.now().minusMinutes(since));
    }

    public List<EcgData> getEcgData(String imei, Long since) {

        Device device = deviceRepository.findByImei(imei).orElseThrow(() -> new RuntimeException("Device not found: " + imei));

        return ecgDataRepository.findRecentEcgData(device, LocalDateTime.now().minusMinutes(since));
    } 

    public List<Heartbeat> getHeartbeats(String imei, Long since) {

        Device device = deviceRepository.findByImei(imei).orElseThrow(() -> new RuntimeException("Device not found: " + imei));

        return heartbeatRepository.findRecentHeartbeats(device, LocalDateTime.now().minusMinutes(since));
    }

    public List<Oxygen> getOxygenLevels(String imei, Long since) {

        Device device = deviceRepository.findByImei(imei).orElseThrow(() -> new RuntimeException("Device not found: " + imei));

        return oxygenRepository.findRecentOxygenLevels(device, LocalDateTime.now().minusMinutes(since));
    }

    public List<Location> getLocations(String imei, Long since) {

        Device device = deviceRepository.findByImei(imei).orElseThrow(() -> new RuntimeException("Device not found: " + imei));

        return locationRepository.findRecentLocations(device, LocalDateTime.now().minusMinutes(since));
    }

    public List<Location> getSOSAlerts(String imei, Long since) {

        Device device = deviceRepository.findByImei(imei).orElseThrow(() -> new RuntimeException("Device not found: " + imei));

        return locationRepository.findLastSOSAlerts(device, LocalDateTime.now().minusMinutes(since));
    }

    public List<FallDownAlert> getFallDownAlerts(String imei, Long since) {

        Device device = deviceRepository.findByImei(imei).orElseThrow(() -> new RuntimeException("Device not found: " + imei));

        return fallDownAlertRepository.findRecentFallDownAlerts(device, LocalDateTime.now().minusMinutes(since));
    }

    public List<SleepEntry> getSleepEntries(String imei, Long since) {

        Device device = deviceRepository.findByImei(imei).orElseThrow(() -> new RuntimeException("Device not found: " + imei));

        return sleepEntryRepository.findRecentSleepEntries(device, LocalDateTime.now().minusMinutes(since));
    }
}
