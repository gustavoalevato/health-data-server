# DeviceService Audio Processing Analysis

## Overview
The `DeviceService` class in the Thalamus Watch Monitor project handles bidirectional audio communication with Touch4P devices. It provides methods for both sending audio TO devices and receiving/streaming audio FROM devices.

---

## 1. sendAudioMessage Method

### Location
**File:** `/home/c900710/ambiente_desenvolvimento/ambiente_desenvolvedor_roboflex/workspace/thalamus-watch-monitor/server/src/main/java/br/com/zontec/watchmonitor/server/service/DeviceService.java`
**Lines:** 180-191

### Full Method Implementation
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

### Processing Flow
1. **Input Validation:** Checks if audioData is null or empty
2. **Device Resolution:** Calls `dispatchCommand()` which:
   - Retrieves the active channel for the IMEI
   - Validates the device exists and channel is active
   - Gets the appropriate OutboundCommandBuilder from factory
3. **Builder Check:** Ensures the builder is a `Touch4POutboundCommandBuilder` (audio sending is Touch4P specific)
4. **Command Building:** Delegates to `Touch4POutboundCommandBuilder.buildAudioMessageCommand()`
5. **Dispatch:** Sends the formatted command through the Netty channel

### Return Value
Returns `DeviceCommandDispatchResult` containing:
- `sent` (boolean): true if command was successfully queued to the device
- `sentFrame` (String): The actual command frame that was sent

### Error Handling
- **IllegalArgumentException:** Thrown if audio data is empty or builder is not Touch4P
- **Validation in dispatchCommand():**
  - Device must be offline → logged warning, returns false
  - Device must exist in database → throws IllegalArgumentException

---

## 2. loadAudioStream Method

### Location
**File:** `/home/c900710/ambiente_desenvolvimento/ambiente_desenvolvedor_roboflex/workspace/thalamus-watch-monitor/server/src/main/java/br/com/zontec/watchmonitor/server/service/DeviceService.java`
**Lines:** 506-521

### Full Method Implementation
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

### Processing Flow
1. **Database Lookup:** Finds InboundAudioMessage record by ID from repository
2. **Path Resolution:** 
   - Converts stored file path to absolute normalized path
   - Validates file exists and is readable
3. **Resource Creation:** Wraps file path in Spring's PathResource
4. **Payload Building:** Creates AudioStreamPayload containing:
   - File name (extracted from path)
   - Spring Resource object (for streaming)
   - File size in bytes
   - Normalized path object
5. **Error Handling:** Catches IOExceptions and wraps in UncheckedIOException

### Return Value
`AudioStreamPayload` record with:
```java
public record AudioStreamPayload(
    String fileName,
    Resource resource,
    long contentLength,
    Path path
)
```

### Error Handling
- **IllegalArgumentException:** 
  - Audio ID not found in database
  - Physical file doesn't exist or is not readable
- **UncheckedIOException:** Wraps IOExceptions during file size calculation

---

## 3. Supporting Audio Methods

### 3.1 saveAudioMessage Method
**Lines:** 476-491

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

**Purpose:** Saves inbound audio messages received from devices
**Flow:**
1. Resolves device by IMEI
2. Delegates file storage to `Touch4PAudioStorageService`
3. Creates and persists InboundAudioMessage database record
4. Updates device's lastSeen timestamp

### 3.2 listRecentAudioMessages Method
**Lines:** 493-504

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

**Purpose:** Retrieves recent audio messages for a device
**Returns:** List of `InboundAudioMessageResponse` with metadata and stream URL

---

## 4. Audio Conversion and Processing Logic

### 4.1 Touch4PAudioMessageCodec Class
**File:** `/home/c900710/ambiente_desenvolvimento/ambiente_desenvolvedor_roboflex/workspace/thalamus-watch-monitor/server/src/main/java/br/com/zontec/watchmonitor/server/utils/Touch4PAudioMessageCodec.java`

This utility class handles the protocol-level encoding/decoding for Touch4P audio messages.

#### Escape Sequences (Protocol Level)
The Touch4P protocol requires escaping certain byte values:

| Byte Value | Hex  | Escaped As       | Purpose                    |
|------------|------|------------------|----------------------------|
| 0x7D       | }    | 0x7D 0x01        | Escape marker itself       |
| 0x5B       | [    | 0x7D 0x02        | Command start delimiter    |
| 0x5D       | ]    | 0x7D 0x03        | Command end delimiter      |
| 0x2C       | ,    | 0x7D 0x04        | Field separator            |
| 0x2A       | *    | 0x7D 0x05        | Parameter separator       |

#### encodeEscapedAudio Method (Lines 47-81)
```java
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
```

**Purpose:** Encodes raw audio bytes for transmission over Touch4P protocol
**Process:**
1. Iterates through each byte in the audio payload
2. Replaces protocol-sensitive bytes with escape sequences
3. Returns ISO-8859-1 encoded string for transmission

#### decodeEscapedAudio Method (Lines 13-45)
```java
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
```

**Purpose:** Decodes escaped audio strings back to raw bytes
**Process:**
1. Converts ISO-8859-1 string to bytes
2. Iterates looking for 0x7D escape marker
3. When escape found, reads next byte and converts escape sequence back to original byte
4. Handles truncated sequences and invalid escape codes with exceptions

### 4.2 Touch4POutboundCommandBuilder Integration
**File:** `/home/c900710/ambiente_desenvolvedor_roboflex/workspace/thalamus-watch-monitor/server/src/main/java/br/com/zontec/watchmonitor/server/protocol/outbound/Touch4POutboundCommandBuilder.java`
**Lines:** 113-119

```java
public String buildAudioMessageCommand(Device device, byte[] audioData) {
    if (audioData == null || audioData.length == 0) {
        throw new IllegalArgumentException("O arquivo de áudio deve ser informado.");
    }

    return wrap(device, "TK," + Touch4PAudioMessageCodec.encodeEscapedAudio(audioData));
}
```

**Flow:**
1. Validates audio data is not empty
2. Calls `encodeEscapedAudio()` to escape protocol-sensitive bytes
3. Wraps result in Touch4P message envelope with format:
   - `[MANUFACTURER*DEVICE_ID*LENGTH*TK,ENCODED_AUDIO]`
   - Where `TK` is the audio command type in Touch4P protocol

### 4.3 Touch4PAudioStorageService
**File:** `/home/c900710/ambiente_desenvolvedor_roboflex/workspace/thalamus-watch-monitor/server/src/main/java/br/com/zontec/watchmonitor/server/service/Touch4PAudioStorageService.java`

```java
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
```

**Features:**
- Configurable storage directory (default: `./data/touch4p/audio`)
- Filename format: `{IMEI}_{TIMESTAMP_yyyyMMddHHmmssSSS}.amr`
- Creates directories if they don't exist
- Returns absolute path of saved file

---

## 5. API Integration Points

### 5.1 Send Audio to Device Endpoint
**Controller:** `DeviceAudioController.java`
**Endpoint:** `POST /api/devices/{imei}/audios/send`
**Method:** `sendAudioToDevice()`

```java
public ResponseEntity<String> sendAudioToDevice(
        @PathVariable String imei,
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

### 5.2 List Device Audios Endpoint
**Endpoint:** `GET /api/devices/{imei}/audios`
**Parameters:** `limit` (default: 20, max: 100)

### 5.3 Stream Audio Endpoint
**Endpoint:** `GET /api/device-audios/{audioId}/stream`
**Content-Type:** `audio/amr`
**Header:** `Content-Disposition: inline; filename="..."`

---

## 6. Audio Processing Summary

### Processing Pipeline

#### Sending Audio TO Device
```
API Upload (MultipartFile)
    ↓
sendAudioMessage(imei, byte[])
    ↓
dispatchCommand() validates device/channel
    ↓
buildAudioMessageCommand()
    ↓
Touch4PAudioMessageCodec.encodeEscapedAudio()
    ├─ Replaces protocol-sensitive bytes with escape sequences
    └─ Returns ISO-8859-1 encoded string
    ↓
Touch4PMessageEnvelope.format()
    └─ Wraps in [MANUFACTURER*DEVICE_ID*LENGTH*TK,ENCODED_AUDIO]
    ↓
Channel.writeAndFlush() → Device
```

#### Receiving Audio FROM Device
```
Device sends TK command with escaped audio
    ↓
GpsWatchProtocolHandler parses message
    ↓
AudioMessageCommand created
    ↓
saveAudioMessage(imei, command)
    ├─ Touch4PAudioMessageCodec.decodeEscapedAudio()
    │  └─ Reverses escape sequences
    ├─ Touch4PAudioStorageService.saveInboundAudio()
    │  └─ Stores as {IMEI}_{TIMESTAMP}.amr file
    └─ InboundAudioMessageRepository.save()
       └─ Persists metadata to database
    ↓
Metadata stored: device, filePath, timestamp
```

#### Streaming Audio to Client
```
API GET /api/device-audios/{audioId}/stream
    ↓
loadAudioStream(audioId)
    ├─ Find InboundAudioMessage by ID
    ├─ Verify file exists and is readable
    └─ Create AudioStreamPayload with Resource
    ↓
Return with Content-Type: audio/amr
    └─ Spring streams file to client
```

---

## 7. Data Models

### AudioMessageCommand (Inbound)
```java
public record AudioMessageCommand(
    String imei,
    String manufacturer,
    String deviceId,
    byte[] audioData,
    String rawMessage
) implements DeviceCommand
```

### InboundAudioMessage (Database Entity)
```java
// Persists:
// - Device reference
// - Physical file path (absolute, normalized)
// - Timestamp of reception
// - ID (primary key)
```

### AudioStreamPayload (Response)
```java
public record AudioStreamPayload(
    String fileName,              // Filename extracted from path
    Resource resource,            // Spring Resource for streaming
    long contentLength,           // File size in bytes
    Path path                     // Normalized absolute path
)
```

### DeviceCommandDispatchResult
```java
public record DeviceCommandDispatchResult(
    boolean sent,                 // true if queued to device
    String sentFrame             // Actual command frame sent
)
```

### InboundAudioMessageResponse (API)
```java
public record InboundAudioMessageResponse(
    Long id,
    String imei,
    String fileName,
    LocalDateTime timestamp,
    String streamUrl             // E.g., "/api/device-audios/10/stream"
)
```

---

## 8. Key Configuration

### Properties
- `touch4p.audio.storage-directory`: Directory for storing inbound audio files (default: `./data/touch4p/audio`)
- `touch4p.default-manufacturer`: Default manufacturer code for commands (default: `3G`)

### Content Types
- Inbound audio format: AMR (Adaptive Multi-Rate)
- File naming: `{IMEI}_{yyyyMMddHHmmssSSS}.amr`
- API streaming: `audio/amr` MIME type

---

## 9. Error Handling Summary

| Scenario | Exception | Message |
|----------|-----------|---------|
| Empty audio data | IllegalArgumentException | "O arquivo de áudio deve ser informado." |
| Non-Touch4P device | IllegalArgumentException | "Envio de áudio TK disponível apenas para dispositivos Touch4P." |
| Device offline | (logged, returns false) | "Falha ao enviar comando: Dispositivo {imei} está offline." |
| Audio ID not found | IllegalArgumentException | "Audio {audioId} não encontrado." |
| File not found/unreadable | IllegalArgumentException | "Arquivo físico do áudio {audioId} não encontrado." |
| File I/O error | UncheckedIOException | "Falha ao abrir o áudio {audioId} para stream." |
| Invalid escape sequence | IllegalArgumentException | "Sequencia de escape TK invalida: 0x7D 0x{XX}" |
| Truncated escape sequence | IllegalArgumentException | "Sequencia de escape TK truncada." |

---

## 10. Testing References

### Unit Tests
- `DeviceServiceTest.java`: Tests audio persistence and streaming
- `Touch4PAudioStorageServiceTest.java`: File storage tests
- `Touch4POutboundCommandBuilderTest.java`: Command building tests
- `Touch4PAudioMessageCodecTest.java`: Encoding/decoding tests

### Test Examples
```java
// Sending audio
DeviceCommandDispatchResult result = deviceService.sendAudioMessage(imei, audioData);
assert result.sent() == true;

// Listing audios
List<InboundAudioMessageResponse> audios = deviceService.listRecentAudioMessages(imei, 20);

// Loading stream
AudioStreamPayload payload = deviceService.loadAudioStream(audioId);
assert payload.fileName().endsWith(".amr");
```

