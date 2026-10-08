package br.com.zontec.watchmonitor.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.zontec.watchmonitor.server.command.AlertCommand;
import br.com.zontec.watchmonitor.server.command.AudioMessageCommand;
import br.com.zontec.watchmonitor.server.api.dto.AudioMessageResponse;
import br.com.zontec.watchmonitor.server.api.dto.InboundAudioMessageResponse;
import br.com.zontec.watchmonitor.server.entities.AudioMessage;
import br.com.zontec.watchmonitor.server.entities.Device;
import br.com.zontec.watchmonitor.server.entities.FallDownAlert;
import br.com.zontec.watchmonitor.server.entities.InboundAudioMessage;
import br.com.zontec.watchmonitor.server.entities.Location;
import br.com.zontec.watchmonitor.server.messaging.HealthDataProducer;
import br.com.zontec.watchmonitor.server.messaging.dto.DeviceHealthEvent;
import br.com.zontec.watchmonitor.server.repositories.AudioMessageRepository;
import br.com.zontec.watchmonitor.server.repositories.DeviceRepository;
import br.com.zontec.watchmonitor.server.repositories.FallDownAlertRepository;
import br.com.zontec.watchmonitor.server.repositories.InboundAudioMessageRepository;
import br.com.zontec.watchmonitor.server.repositories.LocationRepository;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private FallDownAlertRepository fallDownAlertRepository;

    @Mock
    private HealthDataProducer healthDataProducer;

    @Mock
    private InboundLocationService inboundLocationService;

    @Mock
    private Touch4PAudioStorageService touch4PAudioStorageService;

    @Mock
    private AudioMessageRepository audioMessageRepository;

    @InjectMocks
    private DeviceService deviceService;

    
    void shouldPersistAndPublishFallDownAlertWhenBit21IsSet() {
        Device device = new Device();
        device.setId(1L);
        device.setImei("123456789012345");

        when(deviceRepository.findByImei(device.getImei())).thenReturn(Optional.of(device));

        AlertCommand command = new AlertCommand(
                device.getImei(),
                "3G",
                "9012345",
                new BigDecimal("22.570512"),
                new BigDecimal("113.8623267"),
                new BigDecimal("0.00"),
                "154.8",
                100,
                100,
                "00200000",
                Boolean.TRUE,
                "[3G*9012345*010A*AL,241020,085353,V,22.570512,N,113.8623267,E,0.00,154.8,0.0,11,100,100,0,0,00200000]");

        deviceService.processAlert(device.getImei(), command);

        ArgumentCaptor<Location> locationCaptor = ArgumentCaptor.forClass(Location.class);
        verify(locationRepository).save(locationCaptor.capture());
        assertEquals(command.latitude(), locationCaptor.getValue().getLatitude());
        assertEquals(command.longitude(), locationCaptor.getValue().getLongitude());

        ArgumentCaptor<FallDownAlert> alertCaptor = ArgumentCaptor.forClass(FallDownAlert.class);
        verify(fallDownAlertRepository).save(alertCaptor.capture());
        assertEquals(command.statusBits(), alertCaptor.getValue().getStatusBits());
        assertEquals(command.latitude(), alertCaptor.getValue().getLatitude());
        assertEquals(command.longitude(), alertCaptor.getValue().getLongitude());

        ArgumentCaptor<DeviceHealthEvent> eventCaptor = ArgumentCaptor.forClass(DeviceHealthEvent.class);
        verify(healthDataProducer).publish(eventCaptor.capture());
        assertEquals(device.getImei(), eventCaptor.getValue().imei());
        assertEquals("fall_down_alarm", eventCaptor.getValue().type());

        verify(inboundLocationService).sendLocation(eq(device), eq(command.latitude()), eq(command.longitude()), any(LocalDateTime.class));
        verify(deviceRepository).save(device);
    }

    
    void shouldOnlyPersistLocationWhenAlertIsNotFallDown() {
        Device device = new Device();
        device.setId(2L);
        device.setImei("123456789012346");

        when(deviceRepository.findByImei(device.getImei())).thenReturn(Optional.of(device));

        AlertCommand command = new AlertCommand(
                device.getImei(),
                "3G",
                "9012346",
                new BigDecimal("22.570512"),
                new BigDecimal("113.8623267"),
                new BigDecimal("0.00"),
                "154.8",
                100,
                100,
                "00000000",
                Boolean.TRUE,
                "[3G*9012346*010A*AL,241020,085353,V,22.570512,N,113.8623267,E,0.00,154.8,0.0,11,100,100,0,0,00000000]");

        deviceService.processAlert(device.getImei(), command);

        verify(locationRepository).save(any(Location.class));
        verify(fallDownAlertRepository, never()).save(any(FallDownAlert.class));
        verify(healthDataProducer, never()).publish(any(DeviceHealthEvent.class));
    }

    @Test
    void shouldPersistInboundAudioMetadataWithPhysicalPath() {
        Device device = new Device();
        device.setId(3L);
        device.setImei("123456789012347");

        AudioMessageCommand command = new AudioMessageCommand(
                device.getImei(),
                "3G",
                "9012347",
                new byte[] { 'A', 'M', 'R' },
                "[3G*9012347*0006*TK,AMR]");

        Path physicalPath = Paths.get("/tmp/touch4p/123456789012347_20260427120000000.amr");

        when(deviceRepository.findByImei(device.getImei())).thenReturn(Optional.of(device));
        when(touch4PAudioStorageService.saveInboundAudio(device.getImei(), command.audioData())).thenReturn(physicalPath);

        deviceService.saveInboundAudioMessage(device.getImei(), command);

        ArgumentCaptor<AudioMessage> audioCaptor = ArgumentCaptor.forClass(AudioMessage.class);
        verify(audioMessageRepository).save(audioCaptor.capture());
        assertEquals(device, audioCaptor.getValue().getDevice());
        assertEquals(physicalPath.toString(), audioCaptor.getValue().getFilePath());

        verify(deviceRepository).save(device);
    }

    @Test
    void shouldListMostRecentInboundAudiosForDevice() {
        Device device = new Device();
        device.setId(4L);
        device.setImei("123456789012348");

        AudioMessage audio = new AudioMessage();
        audio.setId(10L);
        audio.setDevice(device);
        audio.setFilePath("/tmp/touch4p/123456789012348_20260427120000000.amr");
        audio.setTimestamp(LocalDateTime.of(2026, 4, 27, 12, 0));

        when(deviceRepository.findByImei(device.getImei())).thenReturn(Optional.of(device));
        when(audioMessageRepository.findByDeviceOrderByTimestampDesc(eq(device), any(Pageable.class)))
                .thenReturn(List.of(audio));

        List<AudioMessageResponse> responses = deviceService.listRecentAudioMessages(device.getImei(), 20);

        assertEquals(1, responses.size());
        assertEquals(10L, responses.get(0).id());
        assertEquals("123456789012348_20260427120000000.amr", responses.get(0).fileName());
        assertEquals("/api/device-audios/10/stream", responses.get(0).streamUrl());
    }

    @Test
    void shouldLoadAudioStreamPayloadFromStoredReference() throws IOException {
        Path tempFile = Files.createTempFile("touch4p-audio-", ".amr");

        AudioMessage audio = new AudioMessage();
        audio.setId(11L);
        audio.setFilePath(tempFile.toString());

        when(audioMessageRepository.findById(11L)).thenReturn(Optional.of(audio));

        AudioStreamPayload payload = deviceService.loadAudioStream(11L);

        assertEquals(tempFile.getFileName().toString(), payload.fileName());
    }
}
