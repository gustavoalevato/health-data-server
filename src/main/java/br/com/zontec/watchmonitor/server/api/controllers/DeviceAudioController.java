package br.com.zontec.watchmonitor.server.api.controllers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.zontec.watchmonitor.server.api.dto.AudioMessageResponse;
import br.com.zontec.watchmonitor.server.api.dto.InboundAudioMessageResponse;
import br.com.zontec.watchmonitor.server.service.AudioConverterService;
import br.com.zontec.watchmonitor.server.service.AudioStreamPayload;
import br.com.zontec.watchmonitor.server.service.DeviceCommandDispatchResult;
import br.com.zontec.watchmonitor.server.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeviceAudioController {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final MediaType AUDIO_AMR = MediaType.parseMediaType("audio/amr");
    private static final MediaType AUDIO_MP3 = MediaType.parseMediaType("audio/mpeg");
    private static final String WEBM_OPUS_MIME_TYPE = "audio/webm";

    private final DeviceService deviceService;
    private final AudioConverterService audioConverterService;

    @PostMapping(value = "/devices/{imei}/audios/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Envia um áudio gravado para o dispositivo",
            description = "Recebe um arquivo WebM/Opus ou AMR e converte para AMR antes de enviar ao relógio Touch4P pelo comando TK do protocolo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Áudio aceito e enfileirado para envio."),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido ou dispositivo incompatível."),
            @ApiResponse(responseCode = "404", description = "Dispositivo offline.")
    })
    public ResponseEntity<String> sendAudioToDevice(
            @PathVariable String imei,
            @Parameter(description = "Arquivo de áudio (WebM/Opus ou AMR) para envio ao dispositivo.")
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Arquivo de áudio não informado.");
        }

        try {
            byte[] audioBytes = file.getBytes();
            String mimeType = file.getContentType();

            // Se for WebM/Opus, converter para AMR
            if (mimeType != null && mimeType.contains(WEBM_OPUS_MIME_TYPE)) {
                audioBytes = audioConverterService.convertWebmOpusToAmr(audioBytes);
            }

            // Enviar para dispositivo
            DeviceCommandDispatchResult result = deviceService.sendAudioMessage(imei, audioBytes);
            if (result.sent()) {
                /**
                 * Salvando registro do audio enviado no banco
                 */
                deviceService.saveOutboundAudioMessage(imei, audioBytes, Boolean.TRUE);

                return ResponseEntity.ok("Áudio enfileirado para envio.");
            }

            /**
             * Salvando registro do audio enviado no banco
             */
            deviceService.saveOutboundAudioMessage(imei, audioBytes, Boolean.FALSE);

            return ResponseEntity.status(404).body("Dispositivo offline.");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (java.io.IOException e) {
            return ResponseEntity.badRequest().body("Não foi possível ler o arquivo de áudio.");
        }
    }

    @GetMapping("/devices/{imei}/audios")
    @Operation(summary = "Lista os últimos áudios recebidos de um dispositivo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Parâmetro inválido."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado.")
    })
    public ResponseEntity<List<AudioMessageResponse>> listRecentAudios(
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

    @GetMapping("/device-audios/{audioId}/stream")
    @Operation(summary = "Faz stream de um áudio recebido em formato MP3")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stream iniciado com sucesso em MP3."),
            @ApiResponse(responseCode = "404", description = "Áudio não encontrado.")
    })
    public ResponseEntity<Resource> streamAudio(@PathVariable Long audioId) {
        try {
            AudioStreamPayload payload = deviceService.loadAudioStream(audioId);
            
            // Converter de AMR para MP3 sob demanda
            byte[] amrAudioBytes;
            try {
                amrAudioBytes = java.nio.file.Files.readAllBytes(payload.path());
            } catch (java.io.IOException e) {
                return ResponseEntity.status(500).build();
            }

            byte[] mp3AudioBytes = audioConverterService.convertAmrToMp3(amrAudioBytes);
            
            // Criar stream do áudio convertido
            InputStream inputStream = new ByteArrayInputStream(mp3AudioBytes);
            Resource resource = new InputStreamResource(inputStream);
            
            String fileName = payload.fileName().replaceAll("\\.amr$", ".mp3");
            
            return ResponseEntity.ok()
                    .contentType(AUDIO_MP3)
                    .contentLength(mp3AudioBytes.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
                    
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("Limite inválido.");
        }
        return limit;
    }
}

