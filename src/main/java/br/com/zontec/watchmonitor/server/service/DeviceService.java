package br.com.zontec.watchmonitor.server.service;

import br.com.zontec.watchmonitor.server.repositories.OxygenRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.zontec.watchmonitor.server.api.dto.InboundAudioMessageResponse;
import br.com.zontec.watchmonitor.server.api.dto.AudioMessageResponse;
import br.com.zontec.watchmonitor.server.api.dto.DeviceCommandRequest;
import br.com.zontec.watchmonitor.server.command.BloodPressureCommand;
import br.com.zontec.watchmonitor.server.command.BodyTemperatureCommand;
import br.com.zontec.watchmonitor.server.command.ComprehensiveHealthCommand;
import br.com.zontec.watchmonitor.server.command.EcgDataCommand;
import br.com.zontec.watchmonitor.server.command.HeartRateCommand;
import br.com.zontec.watchmonitor.server.command.AlertCommand;
import br.com.zontec.watchmonitor.server.command.AudioMessageCommand;
import br.com.zontec.watchmonitor.server.command.HeartbeatCommand;
import br.com.zontec.watchmonitor.server.command.LinkKeepCommand;
import br.com.zontec.watchmonitor.server.command.LocationCommand;
import br.com.zontec.watchmonitor.server.command.SleepDataCommand;
import br.com.zontec.watchmonitor.server.command.SleepEntryCommand;
import br.com.zontec.watchmonitor.server.command.Spo2Command;
import br.com.zontec.watchmonitor.server.command.WeatherRequestCommand;
import br.com.zontec.watchmonitor.server.entities.AudioMessage;
import br.com.zontec.watchmonitor.server.entities.BloodPressure;
import br.com.zontec.watchmonitor.server.entities.BodyTemperature;
import br.com.zontec.watchmonitor.server.entities.ComprehensiveHealth;
import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.entities.EcgData;
import br.com.zontec.watchmonitor.server.entities.FallDownAlert;
import br.com.zontec.watchmonitor.server.entities.HeartRate;
import br.com.zontec.watchmonitor.server.entities.Heartbeat;
import br.com.zontec.watchmonitor.server.entities.InboundAudioMessage;
import br.com.zontec.watchmonitor.server.entities.Location;
import br.com.zontec.watchmonitor.server.entities.Oxygen;
import br.com.zontec.watchmonitor.server.entities.SleepEntry;
import br.com.zontec.watchmonitor.server.entities.SleepSession;
import br.com.zontec.watchmonitor.server.entities.WeatherRequest;
import br.com.zontec.watchmonitor.server.enums.AudioMessageDirections;
import br.com.zontec.watchmonitor.server.enums.DeviceStatus;
import br.com.zontec.watchmonitor.server.enums.ParserModel;
import br.com.zontec.watchmonitor.server.messaging.HealthDataProducer;
import br.com.zontec.watchmonitor.server.messaging.dto.BloodPressurePayload;
import br.com.zontec.watchmonitor.server.messaging.dto.BodyTemperaturePayload;
import br.com.zontec.watchmonitor.server.messaging.dto.ComprehensiveHealthPayload;
import br.com.zontec.watchmonitor.server.messaging.dto.DeviceHealthEvent;
import br.com.zontec.watchmonitor.server.messaging.dto.EcgDataPayload;
import br.com.zontec.watchmonitor.server.messaging.dto.FallDownAlertPayload;
import br.com.zontec.watchmonitor.server.messaging.dto.HeartRatePayload;
import br.com.zontec.watchmonitor.server.messaging.dto.HeartbeatPayload;
import br.com.zontec.watchmonitor.server.messaging.dto.OxygenPayload;
import br.com.zontec.watchmonitor.server.messaging.dto.SOSAlertPayload;
import br.com.zontec.watchmonitor.server.messaging.dto.SleepEntryPayload;
import br.com.zontec.watchmonitor.server.messaging.dto.SleepSessionPayload;
import br.com.zontec.watchmonitor.server.protocol.outbound.OutboundCommandBuilder;
import br.com.zontec.watchmonitor.server.protocol.outbound.OutboundCommandBuilderFactory;
import br.com.zontec.watchmonitor.server.protocol.outbound.Touch4POutboundCommandBuilder;
import br.com.zontec.watchmonitor.server.repositories.AudioMessageRepository;
import br.com.zontec.watchmonitor.server.repositories.BloodPressureRepository;
import br.com.zontec.watchmonitor.server.repositories.BodyTemperatureRepository;
import br.com.zontec.watchmonitor.server.repositories.ComprehensiveHealthRepository;
import br.com.zontec.watchmonitor.server.repositories.DeviceRepository;
import br.com.zontec.watchmonitor.server.repositories.EcgDataRepository;
import br.com.zontec.watchmonitor.server.repositories.FallDownAlertRepository;
import br.com.zontec.watchmonitor.server.repositories.HeartRateRepository;
import br.com.zontec.watchmonitor.server.repositories.HeartbeatRepository;
import br.com.zontec.watchmonitor.server.repositories.InboundAudioMessageRepository;
import br.com.zontec.watchmonitor.server.repositories.LocationRepository;
import br.com.zontec.watchmonitor.server.repositories.SleepEntryRepository;
import br.com.zontec.watchmonitor.server.repositories.SleepSessionRepository;
import br.com.zontec.watchmonitor.server.repositories.WeatherRequestRepository;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;

@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    @Value("${device.auth.authorized-imeis:}")
	private String authorizedImeisCsv;

	private Set<String> authorizedImeis;
	private final Map<String, Channel> activeChannels = new ConcurrentHashMap<>();
	private final Map<String, ChannelId> authenticatedChannelsByImei = new ConcurrentHashMap<>();
	private final Map<ChannelId, String> authenticatedImeisByChannelId = new ConcurrentHashMap<>();

    // Injeção de todos os repositórios
    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private HeartbeatRepository heartbeatRepository;

    @Autowired
    private ComprehensiveHealthRepository comprehensiveHealthRepository;

    @Autowired
    private HeartRateRepository heartRateRepository;

    @Autowired
    private BodyTemperatureRepository bodyTemperatureRepository;

    @Autowired
    private SleepSessionRepository sleepSessionRepository;

    @Autowired
    private SleepEntryRepository sleepEntryRepository;

    @Autowired
    private EcgDataRepository ecgDataRepository;

    @Autowired
    private WeatherRequestRepository weatherRequestRepository;
    
    @Autowired
    private BloodPressureRepository bloodPressureRepository;

    @Autowired
    private HealthDataProducer healthDataProducer;

    @Autowired
    private InboundLocationService inboundLocationService;

    @Autowired
    private OutboundCommandBuilderFactory outboundCommandBuilderFactory;

    @Autowired
    private OxygenRepository oxygenRepository;

    @Autowired
    private FallDownAlertRepository fallDownAlertRepository;

    @Autowired
    private Touch4PAudioStorageService touch4PAudioStorageService;

    @Autowired
    private AudioMessageRepository audioMessageRepository;

    // Métodos para gerenciamento de canais (já existentes)
    public void registerChannel(String imei, Channel channel) {
        activeChannels.put(imei, channel);
        log.info("Dispositivo {} registrado no canal {}", imei, channel.id());
    }

    public void unregisterChannel(String imei) {
        activeChannels.remove(imei);
        log.info("Dispositivo {} removido dos canais ativos", imei);
    }

    public boolean sendCommand(String imei, String payload) {
        return dispatchCommand(imei, (device, builder) -> builder.buildCommand(device, payload)).sent();
    }

    public boolean sendLocateCommand(String imei) {
        return dispatchCommand(imei, (device, builder) -> builder.buildLocateCommand(device)).sent();
    }

    public DeviceCommandDispatchResult sendCommand(String imei, DeviceCommandRequest request) {
        return dispatchCommand(imei, (device, builder) -> builder.buildCommand(device, request));
    }

    public DeviceCommandDispatchResult sendAudioMessage(String imei, byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("O arquivo de áudio deve ser informado.");
        }

        return dispatchCommand(imei, (device, builder) -> {
            if (!(builder instanceof Touch4POutboundCommandBuilder touch4PBuilder)) {
                throw new IllegalArgumentException("Envio de áudio TK disponível apenas para dispositivos Touch4P.");
            }

            return touch4PBuilder.buildAudioMessageCommand(device, audioData);
        });
    }

    // Método de autenticação (já existente, mas agora usa o repositório)
    public boolean authenticateDevice(String imei) {
        Device device = resolveDevice(imei, null).orElse(null);
        if (device != null && device.getStatus() == DeviceStatus.ACTIVE) {
            log.info("Device IMEI {} authenticated successfully.", device.getImei());
            return true;
        } else {
            log.warn("Authentication failed for unauthorized IMEI: {}. Please check configuration or database.", imei);
            return false;
        }
    }

    public boolean authenticateDevice(String imei, String parserModel) {
        Device device = resolveDevice(imei, parserModel).orElse(null);
        if (device != null && device.getStatus() == DeviceStatus.ACTIVE) {
            log.info("Device IMEI {} authenticated successfully.", device.getImei());
            return true;
        } else {
            log.warn("Authentication failed for unauthorized IMEI: {}. Please check configuration or database.", imei);
            return false;
        }
    }

    public String resolveImei(String deviceIdentifier, String parserModel) {
        return resolveDevice(deviceIdentifier, parserModel)
                .map(Device::getImei)
                .orElse(deviceIdentifier);
    }

    public String getParserModelByImei(String imei) {
        return resolveDevice(imei, null)
                .map(Device::getParserModel)
                .filter(model -> model != null && !model.isBlank())
                .map(model -> model.trim().toUpperCase())
                .orElse(ParserModel.GPS_WATCH.name());
    }

    private java.util.Optional<Device> resolveDevice(String deviceIdentifier, String parserModel) {
        if (deviceIdentifier == null || deviceIdentifier.isBlank()) {
            return java.util.Optional.empty();
        }

        java.util.Optional<Device> exactMatch = deviceRepository.findByImei(deviceIdentifier);
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        String normalizedParserModel = parserModel == null ? null : parserModel.trim().toUpperCase();
        if (ParserModel.TOUCH_4P.name().equals(normalizedParserModel) && deviceIdentifier.length() == 10) {
            return deviceRepository.findFirstByImeiEndingWithAndParserModel(deviceIdentifier, ParserModel.TOUCH_4P.name());
        }

        return java.util.Optional.empty();
    }

    private Device resolveRequiredDevice(String imei) {
        return deviceRepository.findByImei(imei)
                .orElseThrow(() -> new IllegalArgumentException("Dispositivo " + imei + " não encontrado no cadastro."));
    }

    private DeviceCommandDispatchResult dispatchCommand(String imei, java.util.function.BiFunction<Device, OutboundCommandBuilder, String> commandFactory) {
        Channel channel = activeChannels.get(imei);
        if (channel == null || !channel.isActive()) {
            log.warn("Falha ao enviar comando: Dispositivo {} está offline.", imei);
            return new DeviceCommandDispatchResult(false, null);
        }

        Device device = resolveRequiredDevice(imei);
        OutboundCommandBuilder commandBuilder = outboundCommandBuilderFactory.getBuilder(device.getParserModel());
        String fullCommand = commandFactory.apply(device, commandBuilder);
        channel.writeAndFlush(fullCommand);
        log.info("Comando enviado para {}: {}", imei, fullCommand);
        return new DeviceCommandDispatchResult(true, fullCommand);
    }
    
    /**
	 * Registra um canal como autenticado, associando-o a um IMEI.
	 *
	 * @param imei      O IMEI do dispositivo autenticado.
	 * @param channelId O ID do canal Netty associado.
	 */
	public void registerAuthenticatedChannel(String imei, ChannelId channelId) {
		authenticatedChannelsByImei.put(imei, channelId);
		authenticatedImeisByChannelId.put(channelId, imei);
		log.info("Canal {} registrado para IMEI {}. Total de canais autenticados: {}", channelId, imei,
				authenticatedChannelsByImei.size());
	}

	/**
	 * Remove um canal autenticado quando o dispositivo se desconecta.
	 *
	 * @param channelId O ID do canal Netty que se desconectou.
	 */
	public void unregisterAuthenticatedChannel(ChannelId channelId) {
		String imei = authenticatedImeisByChannelId.remove(channelId);
		if (imei != null) {
			authenticatedChannelsByImei.remove(imei);
			log.info("Canal {} (IMEI: {}) nao registrado. Total de canais autenticados: {}", channelId, imei,
					authenticatedChannelsByImei.size());
		}
	}

    // Método para processar login (já existente)
    public void processDeviceLogin(String imei) {
        log.debug("Business Logic: Device {} is now online.", imei);
    }

    // Método para salvar dados de localização (AP01)
    public void saveLocationData(String imei, LocationCommand command) {
        Device device = resolveRequiredDevice(imei);
        LocalDateTime now = LocalDateTime.now();
        
        Location location = new Location();
        location.setDevice(device);
        location.setLatitude(command.latitude());
        location.setLongitude(command.longitude());
        location.setSpeed(command.speed());
        location.setDirection(command.direction());
        location.setBatteryPercentage(command.batteryPercentage());
        location.setGsmStrength(command.gsmStrength());
        location.setTimestamp(now);
        location.setIsSosAlert(Boolean.FALSE);
        
        locationRepository.save(location);
        
        // Atualizar lastSeen
        device.setLastSeen(now);
        deviceRepository.save(device);

        inboundLocationService.sendLocation(device, location.getLatitude(), location.getLongitude(), now);
        
        log.info("Location data saved for IMEI {}: Lat {}, Lon {}", imei, command.latitude(), command.longitude());
    }

    public void saveSOSAlert(String imei, AlertCommand command) {
        Device device = resolveRequiredDevice(imei);
        LocalDateTime now = LocalDateTime.now();
        
        Location location = new Location();
        location.setDevice(device);
        location.setLatitude(command.latitude());
        location.setLongitude(command.longitude());
        location.setSpeed(command.speed());
        location.setDirection(command.direction());
        location.setBatteryPercentage(command.batteryPercentage());
        location.setGsmStrength(command.gsmStrength());
        location.setTimestamp(now);
        location.setIsSosAlert(command.isSOSAlert());
        
        locationRepository.save(location);
        
        // Atualizar lastSeen
        device.setLastSeen(now);
        deviceRepository.save(device);

        publishHealthEvent(imei, "sos_alert", location.getTimestamp(), new SOSAlertPayload(
                imei,
                location.getLatitude(),
                location.getLongitude()));

        inboundLocationService.sendLocation(device, location.getLatitude(), location.getLongitude(), now);
        
        log.info("Location data saved for IMEI {}: Lat {}, Lon {}", imei, command.latitude(), command.longitude());
    }

    public void processAlert(String imei, AlertCommand command) {
        saveSOSAlert(imei, command);

        if (command.isFallDownAlarm()) {
            saveFallDownAlert(imei, command);
        }
        
    }

    // Método para salvar heartbeat (AP03)
    public void saveHeartbeat(String imei, HeartbeatCommand command) {
        Device device = resolveRequiredDevice(imei);
        device.setLastBatteryPercentage(command.batteryPercentage());
        deviceRepository.save(device);
        
        Heartbeat heartbeat = new Heartbeat();
        heartbeat.setDevice(device);
        heartbeat.setBatteryPercentage(command.batteryPercentage());
        heartbeat.setGsmStrength(command.gsmStrength());
        heartbeat.setNumSatellites(command.numSatellites());
        heartbeat.setSteps(command.steps());
        heartbeat.setStatus(command.status());
        heartbeat.setTimestamp(LocalDateTime.now());
        
        heartbeatRepository.save(heartbeat);
        
        /*publishHealthEvent(imei, "heartbeat", heartbeat.getTimestamp(), new HeartbeatPayload(
                heartbeat.getBatteryPercentage(),
                heartbeat.getGsmStrength(),
                heartbeat.getNumSatellites(),
                heartbeat.getSteps(),
                heartbeat.getStatus(),
                heartbeat.getTimestamp()));*/
        
        // Atualizar lastSeen
        device.setLastSeen(LocalDateTime.now());
        deviceRepository.save(device);
        
        log.info("Heartbeat saved for IMEI {}: Battery {}%, Steps {}", imei, command.batteryPercentage(), command.steps());
    }

    // Método para salvar link keep (AP03)
    public void saveLinkKeep(String imei, LinkKeepCommand command) {
        Device device = resolveRequiredDevice(imei);
        device.setLastBatteryPercentage(command.batteryPercentage());
        deviceRepository.save(device);
        
        Heartbeat heartbeat = new Heartbeat();
        heartbeat.setDevice(device);
        heartbeat.setBatteryPercentage(command.batteryPercentage());
        heartbeat.setGsmStrength(command.gsmStrength());
        heartbeat.setNumSatellites(command.numSatellites());
        heartbeat.setSteps(command.steps());
        heartbeat.setStatus(command.status());
        heartbeat.setTimestamp(LocalDateTime.now());
        
        heartbeatRepository.save(heartbeat);
        publishHealthEvent(imei, "heartbeat", heartbeat.getTimestamp(), new HeartbeatPayload(
                heartbeat.getBatteryPercentage(),
                heartbeat.getGsmStrength(),
                heartbeat.getNumSatellites(),
                heartbeat.getSteps(),
                heartbeat.getStatus(),
                heartbeat.getTimestamp()));
        
        // Atualizar lastSeen
        device.setLastSeen(LocalDateTime.now());
        deviceRepository.save(device);
        
        log.info("Link Keep saved for IMEI {}: Battery {}%, Steps {}", imei, command.batteryPercentage(), command.steps());
    }

    // Método para salvar dados de saúde abrangentes (APHP)
    public void saveComprehensiveHealthData(String imei, ComprehensiveHealthCommand command) {
        Device device = resolveRequiredDevice(imei);
        
        ComprehensiveHealth health = new ComprehensiveHealth();
        health.setDevice(device);
        health.setHeartRate(command.heartRate());
        health.setSystolicBp(command.systolicBloodPressure());
        health.setDiastolicBp(command.diastolicBloodPressure());
        health.setSpo2(command.spo2());
        health.setBloodSugar(command.bloodSugar());
        health.setBodyTemperature(command.bodyTemperature());
        health.setTimestamp(LocalDateTime.now());
        
        comprehensiveHealthRepository.save(health);
        publishHealthEvent(imei, "comprehensive_health", health.getTimestamp(), new ComprehensiveHealthPayload(
                health.getHeartRate(),
                health.getSystolicBp(),
                health.getDiastolicBp(),
                health.getSpo2(),
                health.getBloodSugar(),
                health.getBodyTemperature(),
                health.getTimestamp()));
        
        // Atualizar lastSeen
        device.setLastSeen(LocalDateTime.now());
        deviceRepository.save(device);
        
        log.info("Comprehensive health data saved for IMEI {}: HR={}, BP={}/{}, SpO2={}", 
        		imei, command.heartRate(), command.systolicBloodPressure(), command.diastolicBloodPressure(), command.spo2());
    }

    // Método para salvar frequência cardíaca (AP49)
    public void saveHeartRate(String imei, HeartRateCommand command) {
        Device device = resolveRequiredDevice(imei);
        
        HeartRate heartRate = new HeartRate();
        heartRate.setDevice(device);
        heartRate.setHeartRate(command.heartRate());
        heartRate.setTimestamp(LocalDateTime.now());
        
        heartRateRepository.save(heartRate);
        publishHealthEvent(imei, "heart_rate", heartRate.getTimestamp(), new HeartRatePayload(
                heartRate.getHeartRate(),
                heartRate.getTimestamp()));
        
        // Atualizar lastSeen
        device.setLastSeen(LocalDateTime.now());
        deviceRepository.save(device);
        
        log.info("Heart rate saved for IMEI {}: {} bpm", imei, command.heartRate());
    }

    public void saveOxygen(String imei, Spo2Command command) {
        Device device = resolveRequiredDevice(imei);
        
        Oxygen oxygen = new Oxygen();
        oxygen.setDevice(device);
        oxygen.setType(command.type());
        oxygen.setOxygenLevel(command.spo2());
        oxygen.setTimestamp(LocalDateTime.now());
        
        oxygenRepository.save(oxygen);
        publishHealthEvent(imei, "spo2", oxygen.getTimestamp(), new OxygenPayload(
                oxygen.getOxygenLevel(),
                oxygen.getTimestamp()));
        
        // Atualizar lastSeen
        device.setLastSeen(LocalDateTime.now());
        deviceRepository.save(device);
        
        log.info("SpO2 saved for IMEI {}: {}%", imei, command.spo2());
    }

    public void saveInboundAudioMessage(String imei, AudioMessageCommand command) {
        Device device = resolveRequiredDevice(imei);
        LocalDateTime now = LocalDateTime.now();
        java.nio.file.Path savedPath = touch4PAudioStorageService.saveInboundAudio(imei, command.audioData());

        AudioMessage audioMessage = new AudioMessage();
        audioMessage.setDevice(device);
        audioMessage.setFilePath(savedPath.toString());
        audioMessage.setTimestamp(now);
        audioMessage.setDirection(AudioMessageDirections.INBOUND);
        audioMessageRepository.save(audioMessage);

        device.setLastSeen(now);
        deviceRepository.save(device);

        log.info("Audio TK salvo para IMEI {} em {}", imei, savedPath);
    }

    public void saveOutboundAudioMessage(String imei, byte[] audioData, Boolean sended) {
        Device device = resolveRequiredDevice(imei);
        LocalDateTime now = LocalDateTime.now();
        java.nio.file.Path savedPath = touch4PAudioStorageService.saveInboundAudio(imei, audioData);

        AudioMessage audioMessage = new AudioMessage();
        audioMessage.setDevice(device);
        audioMessage.setFilePath(savedPath.toString());
        audioMessage.setTimestamp(now);
        audioMessage.setDirection(AudioMessageDirections.OUTBOUND);
        audioMessage.setSended(sended);
        audioMessageRepository.save(audioMessage);

        device.setLastSeen(now);
        deviceRepository.save(device);

        log.info("Audio TK salvo para IMEI {} em {}", imei, savedPath);
    }

    public List<AudioMessageResponse> listRecentAudioMessages(String imei, int limit) {
        Device device = resolveRequiredDevice(imei);
        return audioMessageRepository.findByDeviceOrderByTimestampDesc(device, PageRequest.of(0, limit))
                .stream()
                .map(audio -> new AudioMessageResponse(
                        audio.getId(),
                        device.getImei(),
                        Path.of(audio.getFilePath()).getFileName().toString(),
                        audio.getTimestamp(),
                        audio.getDirection().name(),
                        audio.getSended(),
                        "/api/device-audios/" + audio.getId() + "/stream"))
                .toList();
    }

    public AudioStreamPayload loadAudioStream(Long audioId) {
        AudioMessage audioMessage = audioMessageRepository.findById(audioId)
                .orElseThrow(() -> new IllegalArgumentException("Audio " + audioId + " não encontrado."));

        Path path = Path.of(audioMessage.getFilePath()).toAbsolutePath().normalize();
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("Arquivo físico do áudio " + audioId + " não encontrado.");
        }

        Resource resource = new PathResource(path);
        try {
            return new AudioStreamPayload(path.getFileName().toString(), resource, Files.size(path), path);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao abrir o áudio " + audioId + " para stream.", e);
        }
    }

    // Método para salvar temperatura corporal (AP50)
    public void saveBodyTemperature(String imei, BodyTemperatureCommand command) {
        Device device = resolveRequiredDevice(imei);
        
        BodyTemperature bodyTemperature = new BodyTemperature();
        bodyTemperature.setDevice(device);
        bodyTemperature.setTemperature(command.temperature());
        bodyTemperature.setTimestamp(LocalDateTime.now());
        
        bodyTemperatureRepository.save(bodyTemperature);
        publishHealthEvent(imei, "body_temperature", bodyTemperature.getTimestamp(), new BodyTemperaturePayload(
                bodyTemperature.getTemperature(),
                bodyTemperature.getTimestamp()));
        
        // Atualizar lastSeen
        device.setLastSeen(LocalDateTime.now());
        deviceRepository.save(device);
        
        log.info("Body temperature saved for IMEI {}: {}°C", imei, command.temperature());
    }

    // Método para salvar dados de sono (AP97) - Transacional para consistência
    @Transactional
    public void saveSleepData(String imei, SleepDataCommand command) {
        Device device = resolveRequiredDevice(imei);
        
        SleepSession sleepSession = new SleepSession();
        sleepSession.setDevice(device);
        sleepSession.setSleepDate(LocalDate.parse(command.date().toString(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        sleepSession.setStartTime(command.startTime());
        sleepSession.setEndTime(command.endTime());
        sleepSession.setTotalDurationMinutes(command.totalSleepDurationMinutes());
        sleepSession.setTimestamp(LocalDateTime.now());
        
        sleepSessionRepository.save(sleepSession);
        List<SleepEntryPayload> sleepEntriesPayload = new ArrayList<>();
        
        // Salvar entradas de sono
        for (SleepEntryCommand entry : command.sleepEntries()) {
        	SleepEntry sleepEntry = new SleepEntry();
            sleepEntry.setSleepSession(sleepSession);
            sleepEntry.setSleepType(entry.sleepType());
            sleepEntry.setDurationMinutes(entry.durationMinutes());
            sleepEntryRepository.save(sleepEntry);
            sleepEntriesPayload.add(new SleepEntryPayload(
                    sleepEntry.getSleepType(),
                    sleepEntry.getDurationMinutes()));
        }
        publishHealthEvent(imei, "sleep_session", sleepSession.getTimestamp(), new SleepSessionPayload(
                sleepSession.getSleepDate(),
                sleepSession.getStartTime(),
                sleepSession.getEndTime(),
                sleepSession.getTotalDurationMinutes(),
                sleepEntriesPayload,
                sleepSession.getTimestamp()));
        
        // Atualizar lastSeen
        device.setLastSeen(LocalDateTime.now());
        deviceRepository.save(device);
        
        log.info("Sleep data saved for IMEI {}: Date={}, TotalDuration={}min, Entries={}", 
        		imei, command.date(), command.totalSleepDurationMinutes(), command.sleepEntries().size());
    }

    // Método para salvar dados de ECG (APHD)
    public void saveEcgData(String imei, EcgDataCommand command) {
        Device device = resolveRequiredDevice(imei);
        
        EcgData ecgData = new EcgData();
        ecgData.setDevice(device);
        ecgData.setEcgTimestamp(command.timestamp());
        ecgData.setGain(command.gain());
        ecgData.setZeroVoltage(command.zeroVoltage());
        ecgData.setLeadType(command.leadType());
        ecgData.setSamplingRate(command.samplingRate());
        ecgData.setTotalPackets(command.totalPackets());
        ecgData.setCurrentPacket(command.currentPacket());
        ecgData.setDataPoints(command.getEcgDataPointsJson());  // Converter lista para JSON string (assumindo que o campo é String)
        ecgData.setTimestamp(LocalDateTime.now());
        
        ecgDataRepository.save(ecgData);
        publishHealthEvent(imei, "ecg_data", ecgData.getTimestamp(), new EcgDataPayload(
                ecgData.getEcgTimestamp(),
                ecgData.getGain(),
                ecgData.getZeroVoltage(),
                ecgData.getLeadType(),
                ecgData.getSamplingRate(),
                ecgData.getTotalPackets(),
                ecgData.getCurrentPacket(),
                ecgData.getDataPoints(),
                ecgData.getTimestamp()));
        
        // Atualizar lastSeen
        device.setLastSeen(LocalDateTime.now());
        deviceRepository.save(device);
        
        log.info("ECG data saved for IMEI {}: TotalPackets={}, CurrentPacket={}, DataPoints={}", 
        		imei, command.totalPackets(), command.currentPacket(), command.ecgDataPoints().size());
    }

    // Método para processar solicitação de clima (APWT)
    public void processWeatherRequest(String imei, WeatherRequestCommand command) {
        Device device = resolveRequiredDevice(imei);
        
        WeatherRequest weatherRequest = new WeatherRequest();
        weatherRequest.setDevice(device);
        weatherRequest.setCity(command.city());
        weatherRequest.setTimestamp(LocalDateTime.now());
        
        weatherRequestRepository.save(weatherRequest);
        
        // Atualizar lastSeen
        device.setLastSeen(LocalDateTime.now());
        deviceRepository.save(device);
        
        log.info("Weather request processed for IMEI {}: City={}", imei, command.city());
        // Nota: A resposta BPWT já é enviada no GpsWatchProtocolHandler
    }
    
    public void saveBloodPressure(String imei, BloodPressureCommand command) {
        Device device = resolveRequiredDevice(imei);
        
        BloodPressure bloodPressure = new BloodPressure();
        bloodPressure.setDevice(device);
        bloodPressure.setSystolic(command.systolicPressure());
        bloodPressure.setDiastolic(command.diastolicPressure());
        bloodPressure.setTimestamp(LocalDateTime.now());

        bloodPressureRepository.save(bloodPressure);


        HeartRate heartRate = new HeartRate();
        heartRate.setDevice(device);
        heartRate.setHeartRate(command.heartRate());
        heartRate.setTimestamp(LocalDateTime.now());
        
        heartRateRepository.save(heartRate);
        
        
        publishHealthEvent(imei, "blood_pressure", bloodPressure.getTimestamp(), new BloodPressurePayload(
                bloodPressure.getSystolic(),
                bloodPressure.getDiastolic(),
                bloodPressure.getTimestamp()));
        
        // Atualizar lastSeen
        device.setLastSeen(LocalDateTime.now());
        deviceRepository.save(device);
        
        log.info("Blood pressure saved for IMEI {}: Systolic={}, Diastolic={}", imei, command.systolicPressure(), command.diastolicPressure());
    }
    
    public void saveBloodTemperatureData(String imei, BloodPressureCommand command) {
    	
    	Device device = resolveRequiredDevice(imei);
    	
		log.info(
				"Regras de negocio: Salvando location data for IMEI {}: HeartRate {}, SystolicPressure {}, DiastolicPressure {} ",
				imei, command.heartRate(), command.systolicPressure(),
				command.diastolicPressure());
	}

    public void saveFallDownAlert(String imei, AlertCommand command) {
        Device device = resolveRequiredDevice(imei);
        LocalDateTime now = LocalDateTime.now();

        FallDownAlert alert = new FallDownAlert();
        alert.setDevice(device);
        alert.setLatitude(command.latitude());
        alert.setLongitude(command.longitude());
        alert.setSpeed(command.speed());
        alert.setDirection(command.direction());
        alert.setBatteryPercentage(command.batteryPercentage());
        alert.setGsmStrength(command.gsmStrength());
        alert.setStatusBits(command.statusBits());
        alert.setTimestamp(now);

        fallDownAlertRepository.save(alert);
        publishHealthEvent(imei, "fall_down_alarm", alert.getTimestamp(), new FallDownAlertPayload(
                alert.getLatitude(),
                alert.getLongitude(),
                alert.getSpeed(),
                alert.getDirection(),
                alert.getBatteryPercentage(),
                alert.getGsmStrength(),
                alert.getStatusBits(),
                alert.getTimestamp()));

        device.setLastSeen(now);
        deviceRepository.save(device);

        log.info("Fall down alert saved for IMEI {}: statusBits={}, Lat {}, Lon {}",
                imei, command.statusBits(), command.latitude(), command.longitude());
    }

    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    private void publishHealthEvent(String imei, String type, LocalDateTime occurredAt, Object payload) {
        healthDataProducer.publish(new DeviceHealthEvent(imei, type, occurredAt, payload));
    }

}
