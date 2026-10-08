package br.com.zontec.watchmonitor.server.api.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.zontec.watchmonitor.server.api.dto.DeviceCommandRequest;
import br.com.zontec.watchmonitor.server.service.DeviceCommandDispatchResult;
import br.com.zontec.watchmonitor.server.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/devices")
@Tag(name = "Device Commands", description = "Operações de envio de comandos para dispositivos conectados.")
public class DeviceCommandController {

    @Autowired
    private DeviceService deviceService;

    @PostMapping("/{imei}/command")
    @Operation(
            summary = "Envia um comando semântico para o dispositivo",
            description = "Aceita ações tipadas como `LOCATE`, `SET_UPLOAD_INTERVAL`, `CALL`, `MONITOR`, `HEALTH_MONITOR`, `HEALTH_AUTO_SET`, `BODY_TEMPERATURE`, `CENTER`, `APN`, `SET_GMT_BRASILIA` e `RAW`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comando aceito e enfileirado para envio."),
            @ApiResponse(responseCode = "400", description = "Payload inválido ou incompatível com o protocolo do dispositivo."),
            @ApiResponse(responseCode = "404", description = "Dispositivo offline.")
    })
    public ResponseEntity<String> sendCommand(
            @Parameter(description = "IMEI canônico do dispositivo cadastrado.", example = "123458800000015")
            @PathVariable String imei,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Comando semântico tipado.",
                    content = @Content(
                            schema = @Schema(implementation = DeviceCommandRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Locate",
                                            summary = "Solicita localização imediata",
                                            value = "{\"action\":\"LOCATE\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Set Upload Interval",
                                            summary = "Define intervalo de upload",
                                            value = "{\"action\":\"SET_UPLOAD_INTERVAL\",\"intervalSeconds\":600}"
                                    ),
                                    @ExampleObject(
                                            name = "Set Falldown Alert Alarm",
                                            summary = "Configura alarme de queda ativado/desativado",
                                            value = "{\"action\":\"FALLDOWN\",\"fallDownEnabled\":1,\"fallDownAlarmEnabled\":0}"
                                    ),
                                    @ExampleObject(
                                            name = "Call",
                                            summary = "Solicita chamada telefônica",
                                            value = "{\"action\":\"CALL\",\"phoneNumber\":\"11999999999\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Monitor",
                                            summary = "Solicita escuta/monitoramento",
                                            value = "{\"action\":\"MONITOR\",\"phoneNumber\":\"11999999999\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Center",
                                            summary = "Configura número central",
                                            value = "{\"action\":\"CENTER\",\"centerNumber\":\"11999999999\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Apn",
                                            summary = "Configura APN do dispositivo",
                                            value = "{\"action\":\"APN\",\"apnName\":\"cmnet\",\"apnUser\":\"\",\"apnPassword\":\"\",\"mccmncCode\":\"72405\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Set Interval Health Monitor",
                                            summary = "Define intervalo de monitoramento de saúde. Intervalo em segundos",
                                            value = "{\"action\":\"HEALTH_MONITOR\",\"healthMonitorInterval\":6000}"
                                    ),
                                    @ExampleObject(
                                            name = "Set Interval Health Auto Set",
                                            summary = "Define intervalo de configuração automática de saúde. Intervalo em minutos",
                                            value = "{\"action\":\"HEALTH_AUTO_SET\",\"healthAutosetInterval\":180}"
                                    ),
                                    @ExampleObject(
                                            name = "Set GMT Brasília",
                                            summary = "Define o fuso horário para Brasília",
                                            value = "{\"action\":\"SET_GMT_BRASILIA\"}"
                                    ),
                                    @ExampleObject(
                                            name = "bodytemp2",
                                            summary = "Solicita leitura de temperatura corporal usando comando bodytemp2 (apenas Touch4P)",
                                            value = "{\"action\":\"BODY_TEMPERATURE\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Raw",
                                            summary = "Envia payload bruto para o builder do protocolo",
                                            value = "{\"action\":\"RAW\",\"payload\":\"CR\"}"
                                    )
                            }
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody DeviceCommandRequest request) {

        try {
            DeviceCommandDispatchResult result = deviceService.sendCommand(imei, request);
            if (result.sent()) {
                return ResponseEntity.ok("Comando enfileirado para envio: " + result.sentFrame());
            }
            return ResponseEntity.status(404).body("Dispositivo offline.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping("/{imei}/command/raw")
    @Operation(
            summary = "Envia um comando bruto legado",
            description = "Mantido para compatibilidade. Prefira o endpoint tipado `/command`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comando bruto aceito para envio."),
            @ApiResponse(responseCode = "400", description = "Comando bruto ausente ou inválido."),
            @ApiResponse(responseCode = "404", description = "Dispositivo offline.")
    })
    public ResponseEntity<String> sendRawCommand(
            @Parameter(description = "IMEI canônico do dispositivo cadastrado.", example = "123458800000015")
            @PathVariable String imei,
            @org.springframework.web.bind.annotation.RequestBody Map<String, String> body) {

        String commandPayload = body.get("command");
        if (commandPayload == null || commandPayload.isBlank()) {
            return ResponseEntity.badRequest().body("Comando não informado.");
        }

        try {
            boolean success = deviceService.sendCommand(imei, commandPayload);
            if (success) {
                return ResponseEntity.ok("Comando bruto enfileirado para envio.");
            }
            return ResponseEntity.status(404).body("Dispositivo offline.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{imei}/locate")
    @Operation(
            summary = "Atalho para localização imediata",
            description = "Envia o comando semântico de localização usando o protocolo correto para o dispositivo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação de localização enviada."),
            @ApiResponse(responseCode = "400", description = "IMEI inválido ou dispositivo não cadastrado."),
            @ApiResponse(responseCode = "404", description = "Dispositivo offline.")
    })
    public ResponseEntity<String> requestLocation(@PathVariable String imei) {
        try {
            boolean success = deviceService.sendLocateCommand(imei);
            return success
                    ? ResponseEntity.ok("Solicitação de localização enviada.")
                    : ResponseEntity.status(404).body("Dispositivo offline.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
