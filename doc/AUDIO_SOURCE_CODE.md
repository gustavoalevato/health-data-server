# DeviceService Audio - Complete Source Code

## File Structure
```
src/main/java/br/com/zontec/watchmonitor/server/
├── service/
│   ├── DeviceService.java                    [Main service class]
│   ├── AudioStreamPayload.java               [Stream payload record]
│   └── Touch4PAudioStorageService.java       [File storage service]
├── protocol/outbound/
│   └── Touch4POutboundCommandBuilder.java    [Command builder with audio support]
├── utils/
│   └── Touch4PAudioMessageCodec.java         [Protocol encoding/decoding]
├── api/controllers/
│   └── DeviceAudioController.java            [REST endpoints]
├── command/
│   └── AudioMessageCommand.java              [Command DTO]
├── entities/
│   └── InboundAudioMessage.java              [Database entity]
└── repositories/
    └── InboundAudioMessageRepository.java    [JPA repository]
```

---

## 1. DeviceService.java - Core Methods

### Location
`/home/c900710/ambiente_desenvolvimento/ambiente_desenvolvedor_roboflex/workspace/thalamus-watch-monitor/server/src/main/java/br/com/zontec/watchmonitor/server/service/DeviceService.java`

### Import Statements (Relevant to Audio)
```java
import br.com.zontec.watchmonitor.server.api.dto.InboundAudioMessageResponse;
import br.com.zontec.watchmonitor.server.command.AudioMessageCommand;
import br.com.zontec.watchmonitor.server.entities.InboundAudioMessage;
import br.com.zontec.watchmonitor.server.repositories.InboundAudioMessageRepository;
import br.com.zontec.watchmonitor.server.service.Touch4PAudioStorageService;
import org.springframework.core.io.PathResource;
import org.springframework.data.domain.PageRequest;
```

### sendAudioMessage Method (Lines 180-191)
```java
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
```

### saveAudioMessage Method (Lines 476-491)
```java
public void saveAudioMessage(String imei, AudioMessageCommand command) {
    Device device = resolveRequiredDevice(imei);
    LocalDateTime now = LocalDateTime.now();
    java.nio.file.Path savedPath = touch4PAudioStorageService.saveInboundAudio(imei, command.audioData());

    InboundAudioMessage audioMessage = new InboundAudioMessage();
    audioMessage.setDevice(device);
    audioMessage.setFilePath(savedPath.toString());
    audioMessage.setTimestamp(now);
    inboundAudioMessageRepository.save(audioMessage);

    device.setLastSeen(now);
    deviceRepository.save(device);

    log.info("Audio TK salvo para IMEI {} em {}", imei, savedPath);
}
```

### listRecentAudioMessages Method (Lines 493-504)
```java
public List<InboundAudioMessageResponse> listRecentAudioMessages(String imei, int limit) {
    Device device = resolveRequiredDevice(imei);
    return inboundAudioMessageRepository.findByDeviceOrderByTimestampDesc(device, PageRequest.of(0, limit))
            .stream()
            .map(audio -> new InboundAudioMessageResponse(
                    audio.getId(),
                    device.getImei(),
                    Path.of(audio.getFilePath()).getFileName().toString(),
                    audio.getTimestamp(),
                    "/api/device-audios/" + audio.getId() + "/stream"))
            .toList();
}
```

### loadAudioStream Method (Lines 506-521)
```java
public AudioStreamPayload loadAudioStream(Long audioId) {
    InboundAudioMessage audioMessage = inboundAudioMessageRepository.findById(audioId)
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
```

---

## 2. Touch4PAudioMessageCodec.java - Encoding/Decoding

### Complete File
`/home/c900710/ambiente_desenvolvimento/ambiente_desenvolvedor_roboflex/workspace/thalamus-watch-monitor/server/src/main/java/br/com/zontec/watchmonitor/server/utils/Touch4PAudioMessageCodec.java`

```java
package br.com.zontec.watchmonitor.server.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class Touch4PAudioMessageCodec {

    private static final int ESCAPE = 0x7D;

    private Touch4PAudioMessageCodec() {
    }

    public static byte[] decodeEscapedAudio(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload de audio nao informado.");
        }

        byte[] encoded = payload.getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream decoded = new ByteArrayOutputStream(encoded.length);

        for (int i = 0; i < encoded.length; i++) {
            int current = encoded[i] & 0xFF;
            if (current != ESCAPE) {
                decoded.write(current);
                continue;
            }

            if (i + 1 >= encoded.length) {
                throw new IllegalArgumentException("Sequencia de escape TK truncada.");
            }

            int next = encoded[++i] & 0xFF;
            decoded.write(switch (next) {
                case 0x01 -> 0x7D;
                case 0x02 -> 0x5B;
                case 0x03 -> 0x5D;
                case 0x04 -> 0x2C;
                case 0x05 -> 0x2A;
                default -> throw new IllegalArgumentException("Sequencia de escape TK invalida: 0x7D 0x"
                        + String.format("%02X", next));
            });
        }

        return decoded.toByteArray();
    }

    public static String encodeEscapedAudio(byte[] payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload de audio nao informado.");
        }

        ByteArrayOutputStream encoded = new ByteArrayOutputStream(payload.length);
        for (byte value : payload) {
            int current = value & 0xFF;
            switch (current) {
                case 0x7D -> {
                    encoded.write(0x7D);
                    encoded.write(0x01);
                }
                case 0x5B -> {
                    encoded.write(0x7D);
                    encoded.write(0x02);
                }
                case 0x5D -> {
                    encoded.write(0x7D);
                    encoded.write(0x03);
                }
                case 0x2C -> {
                    encoded.write(0x7D);
                    encoded.write(0x04);
                }
                case 0x2A -> {
                    encoded.write(0x7D);
                    encoded.write(0x05);
                }
                default -> encoded.write(current);
            }
        }

        return encoded.toString(StandardCharsets.ISO_8859_1);
    }
}
```

---

## 3. Touch4POutboundCommandBuilder.java - Audio Command Building

### Location
`/home/c900710/ambiente_desenvolvimento/ambiente_desenvolvedor_roboflex/workspace/thalamus-watch-monitor/server/src/main/java/br/com/zontec/watchmonitor/server/protocol/outbound/Touch4POutboundCommandBuilder.java`

### buildAudioMessageCommand Method (Lines 113-119)
```java
public String buildAudioMessageCommand(Device device, byte[] audioData) {
    if (audioData == null || audioData.length == 0) {
        throw new IllegalArgumentException("O arquivo de áudio deve ser informado.");
    }

    return wrap(device, "TK," + Touch4PAudioMessageCodec.encodeEscapedAudio(audioData));
}
```

### Helper Methods
```java
private String wrap(Device device, String content) {
    return Touch4PMessageEnvelope.format(resolveManufacturer(), resolveDeviceId(device), content);
}

private String resolveManufacturer() {
    return defaultManufacturer == null || defaultManufacturer.isBlank()
            ? "3G"
            : defaultManufacturer.trim().toUpperCase();
}

private String resolveDeviceId(Device device) {
    String imei = device.getImei();
    if (imei == null || imei.isBlank()) {
        throw new IllegalArgumentException("Dispositivo sem IMEI para montagem do comando Touch4P.");
    }

    String digitsOnly = imei.trim();
    if (digitsOnly.length() < 10) {
        throw new IllegalArgumentException("IMEI inválido para montagem do deviceId Touch4P: " + imei);
    }

    return digitsOnly.substring(digitsOnly.length() - 10);
}
```

---

## 4. Touch4PAudioStorageService.java - File Storage

### Complete File
`/home/c900710/ambiente_desenvolvimento/ambiente_desenvolvedor_roboflex/workspace/thalamus-watch-monitor/server/src/main/java/br/com/zontec/watchmonitor/server/service/Touch4PAudioStorageService.java`

```java
package br.com.zontec.watchmonitor.server.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Touch4PAudioStorageService {

    private static final DateTimeFormatter FILE_NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final Path storageDirectory;

    public Touch4PAudioStorageService(@Value("${touch4p.audio.storage-directory:./data/touch4p/audio}") String storageDirectory) {
        this.storageDirectory = Paths.get(storageDirectory).toAbsolutePath().normalize();
    }

    public Path saveInboundAudio(String imei, byte[] audioData) {
        if (imei == null || imei.isBlank()) {
            throw new IllegalArgumentException("IMEI nao informado para gravacao do audio TK.");
        }
        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("Audio TK vazio.");
        }

        String filename = imei + "_" + LocalDateTime.now().format(FILE_NAME_TIMESTAMP) + ".amr";
        Path target = storageDirectory.resolve(filename);

        try {
            Files.createDirectories(storageDirectory);
            return Files.write(target, audioData);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar audio TK em disco.", e);
        }
    }

    Path storageDirectory() {
        return storageDirectory;
    }
}
```

---

## 5. AudioStreamPayload.java - Response Record

### Complete File
`/home/c900710/ambiente_desenvolvimento/ambiente_desenvolvedor_roboflex/workspace/thalamus-watch-monitor/server/src/main/java/br/com/zontec/watchmonitor/server/service/AudioStreamPayload.java`

```java
package br.com.zontec.watchmonitor.server.service;

import java.nio.file.Path;

import org.springframework.core.io.Resource;

public record AudioStreamPayload(
        String fileName,
        Resource resource,
        long contentLength,
        Path path
) {
}
```

---

## 6. DeviceAudioController.java - API Endpoints

### Location
`/home/c900710/ambiente_desenvolvimento/ambiente_desenvolvedor_roboflex/workspace/thalamus-watch-monitor/server/src/main/java/br/com/zontec/watchmonitor/server/api/controllers/DeviceAudioController.java`

### Complete sendAudioToDevice Method (Lines 37-66)
```java
@PostMapping(value = "/devices/{imei}/audios/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@Operation(
        summary = "Envia um áudio gravado para o dispositivo",
        description = "Recebe um arquivo AMR e envia ao relógio Touch4P pelo comando TK do protocolo."
)
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Áudio aceito e enfileirado para envio."),
        @ApiResponse(responseCode = "400", description = "Arquivo inválido ou dispositivo incompatível."),
        @ApiResponse(responseCode = "404", description = "Dispositivo offline.")
})
public ResponseEntity<String> sendAudioToDevice(
        @PathVariable String imei,
        @Parameter(description = "Arquivo AMR gravado para envio ao dispositivo.")
        @RequestParam("file") MultipartFile file) {
    if (file == null || file.isEmpty()) {
        return ResponseEntity.badRequest().body("Arquivo de áudio não informado.");
    }

    try {
        DeviceCommandDispatchResult result = deviceService.sendAudioMessage(imei, file.getBytes());
        if (result.sent()) {
            return ResponseEntity.ok("Áudio enfileirado para envio.");
        }
        return ResponseEntity.status(404).body("Dispositivo offline.");
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    } catch (java.io.IOException e) {
        return ResponseEntity.badRequest().body("Não foi possível ler o arquivo de áudio.");
    }
}
```

### Complete streamAudio Method (Lines 89-106)
```java
@GetMapping("/device-audios/{audioId}/stream")
@Operation(summary = "Faz stream de um áudio recebido")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stream iniciado com sucesso."),
        @ApiResponse(responseCode = "404", description = "Áudio não encontrado.")
})
public ResponseEntity<org.springframework.core.io.Resource> streamAudio(@PathVariable Long audioId) {
    try {
        AudioStreamPayload payload = deviceService.loadAudioStream(audioId);
        return ResponseEntity.ok()
                .contentType(AUDIO_AMR)
                .contentLength(payload.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + payload.fileName() + "\"")
                .body(payload.resource());
    } catch (IllegalArgumentException e) {
        return ResponseEntity.notFound().build();
    }
}
```

### Complete listRecentAudios Method (Lines 68-87)
```java
@GetMapping("/devices/{imei}/audios")
@Operation(summary = "Lista os últimos áudios recebidos de um dispositivo")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso."),
        @ApiResponse(responseCode = "400", description = "Parâmetro inválido."),
        @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado.")
})
public ResponseEntity<List<InboundAudioMessageResponse>> listRecentAudios(
        @PathVariable String imei,
        @Parameter(description = "Quantidade máxima de áudios retornados.", example = "20")
        @RequestParam(defaultValue = "20") Integer limit) {
    try {
        return ResponseEntity.ok(deviceService.listRecentAudioMessages(imei, normalizeLimit(limit)));
    } catch (IllegalArgumentException e) {
        if (e.getMessage() != null && e.getMessage().contains("não encontrado")) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.badRequest().build();
    }
}
```

---

## 7. AudioMessageCommand.java - Command DTO

### Complete File
`/home/c900710/ambiente_desenvolvimento/ambiente_desenvolvedor_roboflex/workspace/thalamus-watch-monitor/server/src/main/java/br/com/zontec/watchmonitor/server/command/AudioMessageCommand.java`

```java
package br.com.zontec.watchmonitor.server.command;

import br.com.zontec.watchmonitor.server.command.types.GPSWatchCommandType;

public record AudioMessageCommand(
        String imei,
        String manufacturer,
        String deviceId,
        byte[] audioData,
        String rawMessage
) implements DeviceCommand {

    @Override
    public GPSWatchCommandType getType() {
        return GPSWatchCommandType.AUDIO_MESSAGE;
    }

    @Override
    public String getImei() {
        return imei;
    }

    @Override
    public String getRawMessage() {
        return rawMessage;
    }
}
```

---

## 8. DeviceCommandDispatchResult.java - Result Record

### Complete File
`/home/c900710/ambiente_desenvolvimento/ambiente_desenvolvedor_roboflex/workspace/thalamus-watch-monitor/server/src/main/java/br/com/zontec/watchmonitor/server/service/DeviceCommandDispatchResult.java`

```java
package br.com.zontec.watchmonitor.server.service;

public record DeviceCommandDispatchResult(
        boolean sent,
        String sentFrame
) {
}
```

---

## Supporting Methods in DeviceService

### dispatchCommand Method (Lines 253-266)
```java
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
```

### resolveRequiredDevice Method (Lines 248-251)
```java
private Device resolveRequiredDevice(String imei) {
    return deviceRepository.findByImei(imei)
            .orElseThrow(() -> new IllegalArgumentException("Dispositivo " + imei + " não encontrado no cadastro."));
}
```

---

## Initialization and Injection

### DeviceService Audio Dependencies (Lines 152-155)
```java
@Autowired
private Touch4PAudioStorageService touch4PAudioStorageService;

@Autowired
private InboundAudioMessageRepository inboundAudioMessageRepository;
```

### DeviceAudioController Injection (Lines 28-35)
```java
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeviceAudioController {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final MediaType AUDIO_AMR = MediaType.parseMediaType("audio/amr");

    private final DeviceService deviceService;
```

---

## Configuration Constants

### Touch4PAudioStorageService
- Default storage: `./data/touch4p/audio`
- Timestamp format: `yyyyMMddHHmmssSSS`
- File extension: `.amr`

### Touch4POutboundCommandBuilder
- Command type: `TK` (Touch4P audio command)
- Default manufacturer: `3G`
- Device ID length: Last 10 digits of IMEI

### DeviceAudioController
- Audio MIME type: `audio/amr`
- Default limit: 20 files
- Maximum limit: 100 files

---

## File Sizes and Complexity

| File | Lines | Complexity |
|------|-------|-----------|
| DeviceService.java | 714 | High (714 total, ~50 audio-related) |
| Touch4PAudioMessageCodec.java | 82 | Medium (encoding/decoding logic) |
| Touch4POutboundCommandBuilder.java | 152 | Medium (command building) |
| Touch4PAudioStorageService.java | 47 | Low (simple file I/O) |
| DeviceAudioController.java | 117 | Medium (3 audio endpoints) |
| AudioStreamPayload.java | 13 | Low (simple record) |
| AudioMessageCommand.java | 27 | Low (simple DTO) |
| DeviceCommandDispatchResult.java | 7 | Low (simple record) |

