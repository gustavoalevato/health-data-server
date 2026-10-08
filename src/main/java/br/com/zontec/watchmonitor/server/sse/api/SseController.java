package br.com.zontec.watchmonitor.server.sse.api;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import br.com.zontec.watchmonitor.server.sse.api.dto.SseDispatchResponse;
import br.com.zontec.watchmonitor.server.sse.api.dto.SsePublishRequest;
import br.com.zontec.watchmonitor.server.sse.application.SseMessagingService;
import br.com.zontec.watchmonitor.server.sse.domain.SseEventPayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/sse")
@Tag(name = "SSE", description = "Assinatura e publicação de eventos Server-Sent Events.")
public class SseController {

    private final SseMessagingService sseMessagingService;

    public SseController(SseMessagingService sseMessagingService) {
        this.sseMessagingService = sseMessagingService;
    }

    @GetMapping(path = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Assina um ou mais canais SSE",
            description = "Abre uma conexão SSE autenticada para o usuário do token Bearer. Quando nenhum canal é informado, o canal padrão é usado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stream SSE aberto com sucesso."),
            @ApiResponse(responseCode = "401", description = "Token inválido ou ausente.")
    })
    public SseEmitter subscribe(@RequestParam(name = "channel", required = false) List<String> channels,
            Principal principal) {
        return sseMessagingService.subscribe(principal, channels);
    }

    @PostMapping("/channels/{channel}/broadcast")
    @Operation(
            summary = "Publica um evento para um canal SSE",
            description = "Envia o mesmo evento para todos os clientes atualmente inscritos no canal informado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento enfileirado para o canal."),
            @ApiResponse(responseCode = "400", description = "Payload inválido.")
    })
    public ResponseEntity<SseDispatchResponse> broadcastToChannel(
            @PathVariable String channel,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Evento SSE a ser enviado.",
                    content = @Content(
                            schema = @Schema(implementation = SsePublishRequest.class),
                            examples = @ExampleObject(value = "{\"event\":\"device-update\",\"id\":\"evt-1\",\"data\":{\"imei\":\"123\",\"status\":\"ONLINE\"}}")
                    )
            )
            @RequestBody SsePublishRequest request) {
        SseEventPayload payload = toPayload(request);
        int subscribers = sseMessagingService.broadcast(channel, payload);
        return ResponseEntity.ok(new SseDispatchResponse(channel, payload.event(), subscribers));
    }

    @PostMapping("/users/{userId}/send")
    @Operation(
            summary = "Envia um evento SSE para um usuário específico",
            description = "Entrega o evento a todas as conexões SSE ativas associadas ao usuário informado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento enfileirado para o usuário."),
            @ApiResponse(responseCode = "400", description = "Payload inválido.")
    })
    public ResponseEntity<SseDispatchResponse> sendToUser(
            @PathVariable String userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Evento SSE a ser enviado.",
                    content = @Content(
                            schema = @Schema(implementation = SsePublishRequest.class),
                            examples = @ExampleObject(value = "{\"event\":\"private-notification\",\"id\":\"evt-2\",\"data\":{\"message\":\"hello\"}}")
                    )
            )
            @RequestBody SsePublishRequest request) {
        SseEventPayload payload = toPayload(request);
        int subscribers = sseMessagingService.sendToUser(userId, payload);
        return ResponseEntity.ok(new SseDispatchResponse(userId, payload.event(), subscribers));
    }

    @GetMapping("/stats")
    @Operation(summary = "Retorna métricas básicas do módulo SSE")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(sseMessagingService.stats());
    }

    private SseEventPayload toPayload(SsePublishRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payload SSE não informado.");
        }
        return new SseEventPayload(request.event(), request.id(), request.data());
    }
}
