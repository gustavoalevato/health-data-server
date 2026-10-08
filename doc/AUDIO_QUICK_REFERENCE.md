# DeviceService Audio - Quick Reference

## Key Methods

### 1. sendAudioMessage(String imei, byte[] audioData) → DeviceCommandDispatchResult
**Lines:** 180-191
**Purpose:** Send audio file TO a Touch4P device
**Flow:**
- Validates audio data not empty
- Gets device's active Netty channel
- Encodes audio with Touch4PAudioMessageCodec.encodeEscapedAudio()
- Wraps in protocol envelope [3G*DEVICE_ID*LENGTH*TK,ENCODED_DATA]
- Sends via channel.writeAndFlush()

### 2. loadAudioStream(Long audioId) → AudioStreamPayload
**Lines:** 506-521
**Purpose:** Retrieve audio stream for client playback
**Flow:**
- Loads InboundAudioMessage from database
- Validates file exists and is readable
- Creates Spring PathResource wrapper
- Returns AudioStreamPayload with fileName, resource, size, path

### 3. saveAudioMessage(String imei, AudioMessageCommand command) → void
**Lines:** 476-491
**Purpose:** Save inbound audio FROM device
**Flow:**
- Decodes escaped audio with Touch4PAudioMessageCodec.decodeEscapedAudio()
- Saves raw bytes to disk via Touch4PAudioStorageService
- Creates InboundAudioMessage database record
- Updates device's lastSeen timestamp

### 4. listRecentAudioMessages(String imei, int limit) → List<InboundAudioMessageResponse>
**Lines:** 493-504
**Purpose:** List device's recent audio messages with streaming URLs

---

## Audio Encoding/Decoding

### Protocol Escape Sequences (0x7D prefix)
| Original Byte | Hex  | Escaped Sequence | Reason                    |
|---------------|------|------------------|---------------------------|
| 0x7D          | }    | 0x7D 0x01        | Escape marker itself      |
| 0x5B          | [    | 0x7D 0x02        | Protocol delimiter        |
| 0x5D          | ]    | 0x7D 0x03        | Protocol delimiter        |
| 0x2C          | ,    | 0x7D 0x04        | Field separator           |
| 0x2A          | *    | 0x7D 0x05        | Parameter separator       |

### Codec Methods
- **encodeEscapedAudio(byte[])** → String: Prepare for transmission
- **decodeEscapedAudio(String)** → byte[]: Extract from received message

---

## File Storage

### Path Pattern
```
{STORAGE_DIRECTORY}/{IMEI}_{TIMESTAMP_yyyyMMddHHmmssSSS}.amr
```

### Default Storage
- **Directory:** `./data/touch4p/audio`
- **Configurable via:** `touch4p.audio.storage-directory` property
- **Format:** AMR (Adaptive Multi-Rate)

---

## API Endpoints

### Send Audio to Device
```
POST /api/devices/{imei}/audios/send
Content-Type: multipart/form-data

Parameter: file (MultipartFile)

Response:
200: "Áudio enfileirado para envio."
404: "Dispositivo offline."
400: [Validation errors]
```

### List Device Audios
```
GET /api/devices/{imei}/audios?limit=20

Parameters:
- limit: 1-100 (default: 20)

Response: List<InboundAudioMessageResponse>
```

### Stream Audio
```
GET /api/device-audios/{audioId}/stream

Response:
- Content-Type: audio/amr
- Content-Disposition: inline; filename="..."
- Binary stream data
```

---

## Data Flow Diagrams

### Sending (TO Device)
```
User Upload
    ↓ (MultipartFile)
POST /api/devices/{imei}/audios/send
    ↓
sendAudioMessage(imei, bytes)
    ↓
encodeEscapedAudio(bytes) [escape protocol-sensitive bytes]
    ↓
[3G*DEVICE_ID*LENGTH*TK,ENCODED_AUDIO]
    ↓
channel.writeAndFlush()
    ↓
Device receives audio via TK command
```

### Receiving (FROM Device)
```
Device sends TK command
    ↓
GpsWatchProtocolHandler parses
    ↓
AudioMessageCommand(audioData=escaped_bytes)
    ↓
saveAudioMessage(imei, command)
    ├─ decodeEscapedAudio() [reverse escaping]
    ├─ Touch4PAudioStorageService.saveInboundAudio()
    │  └─ Files.write({IMEI}_{TIMESTAMP}.amr)
    ├─ InboundAudioMessage.save()
    │  └─ Database record with filePath reference
    └─ device.setLastSeen()
```

### Playback (Streaming)
```
User Request
    ↓
GET /api/device-audios/{audioId}/stream
    ↓
loadAudioStream(audioId)
    ├─ Find in database
    ├─ Verify file exists
    └─ Create PathResource
    ↓
Return with audio/amr MIME type
    ↓
Browser/Client plays audio
```

---

## Error Scenarios

| Scenario | Code | Exception | Resolution |
|----------|------|-----------|------------|
| No audio file | 400 | IllegalArgumentException | Provide valid audio bytes |
| Wrong device type | 400 | "Envio de áudio TK disponível apenas..." | Only Touch4P supports audio |
| Device offline | 404 | Dispatch returns sent=false | Wait for device to reconnect |
| Invalid audio ID | 404 | IllegalArgumentException | Use valid audioId from list |
| File missing on disk | 404 | Persisted path doesn't exist | Check storage directory |
| Protocol decode error | 400 | Invalid escape sequence | Verify audio format |

---

## Key Classes

| Class | File | Purpose |
|-------|------|---------|
| DeviceService | service/DeviceService.java | Main audio service logic |
| AudioStreamPayload | service/AudioStreamPayload.java | Stream response wrapper |
| Touch4PAudioMessageCodec | utils/Touch4PAudioMessageCodec.java | Encode/decode protocol escapes |
| Touch4POutboundCommandBuilder | protocol/outbound/Touch4POutboundCommandBuilder.java | Build TK commands |
| Touch4PAudioStorageService | service/Touch4PAudioStorageService.java | File persistence |
| DeviceAudioController | api/controllers/DeviceAudioController.java | HTTP endpoints |
| InboundAudioMessage | entities/InboundAudioMessage.java | Database entity |
| AudioMessageCommand | command/AudioMessageCommand.java | Command DTO |

---

## Configuration Properties

```properties
# Audio storage directory
touch4p.audio.storage-directory=./data/touch4p/audio

# Device manufacturer code for outbound commands
touch4p.default-manufacturer=3G
```

---

## Dependencies Injected

```java
// In DeviceService
@Autowired
private Touch4PAudioStorageService touch4PAudioStorageService;

@Autowired
private InboundAudioMessageRepository inboundAudioMessageRepository;

@Autowired
private DeviceRepository deviceRepository;

@Autowired
private OutboundCommandBuilderFactory outboundCommandBuilderFactory;
```

---

## Example Usage

### Send audio to device
```java
byte[] audioBytes = Files.readAllBytes(Path.of("message.amr"));
DeviceCommandDispatchResult result = deviceService.sendAudioMessage("123456789012345", audioBytes);
if (result.sent()) {
    System.out.println("Queued: " + result.sentFrame());
}
```

### List device audios
```java
List<InboundAudioMessageResponse> audios = deviceService.listRecentAudioMessages("123456789012345", 20);
audios.forEach(audio -> System.out.println(audio.fileName() + " - " + audio.streamUrl()));
```

### Stream audio
```java
AudioStreamPayload payload = deviceService.loadAudioStream(42L);
System.out.println("File: " + payload.fileName() + " (" + payload.contentLength() + " bytes)");
// payload.resource() can be used to stream the file
```

---

## Testing

### Key test files
- `DeviceServiceTest.java` - Audio persistence & streaming
- `Touch4PAudioMessageCodecTest.java` - Encoding/decoding
- `Touch4PAudioStorageServiceTest.java` - File storage
- `Touch4POutboundCommandBuilderTest.java` - Command building

### Sample test
```java
@Test
void shouldSendAudioMessageToDevice() {
    byte[] audioData = new byte[] { 'A', 'M', 'R' };
    DeviceCommandDispatchResult result = deviceService.sendAudioMessage(imei, audioData);
    assertTrue(result.sent());
}
```

