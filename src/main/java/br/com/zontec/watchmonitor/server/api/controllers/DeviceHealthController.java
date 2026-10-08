package br.com.zontec.watchmonitor.server.api.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.zontec.watchmonitor.server.entities.BloodPressure;
import br.com.zontec.watchmonitor.server.entities.BodyTemperature;
import br.com.zontec.watchmonitor.server.entities.ComprehensiveHealth;
import br.com.zontec.watchmonitor.server.entities.EcgData;
import br.com.zontec.watchmonitor.server.entities.FallDownAlert;
import br.com.zontec.watchmonitor.server.entities.HeartRate;
import br.com.zontec.watchmonitor.server.entities.Heartbeat;
import br.com.zontec.watchmonitor.server.entities.Location;
import br.com.zontec.watchmonitor.server.entities.Oxygen;
import br.com.zontec.watchmonitor.server.entities.SleepEntry;
import br.com.zontec.watchmonitor.server.entities.SleepSession;
import br.com.zontec.watchmonitor.server.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/health/{imei}")
@RequiredArgsConstructor
@Tag(name = "Device Health", description = "Consultas de dados de saúde, localização e eventos recebidos dos dispositivos.")
public class DeviceHealthController {

    private static final Long SINCE = 1440L; // 1 dia

    @Autowired
    private HealthService healthService;

    @GetMapping("/comprehensive-health")
    @Operation(
            summary = "Lista dados consolidados de saúde",
            description = "Retorna os registros recentes de saúde consolidada do dispositivo, incluindo batimentos, pressão, SpO2, glicemia e temperatura quando disponíveis."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados consolidados retornados com sucesso."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado ou sem dados.")
    })
    public ResponseEntity<List<ComprehensiveHealth>> getComprehensiveHealth(
            @Parameter(description = "IMEI canônico do dispositivo cadastrado.", example = "123458800000015")
            @PathVariable("imei") String imei) {
        
        List<ComprehensiveHealth> health = healthService.getComprehensiveHealth(imei, SINCE);

        if (health != null) {
            return ResponseEntity.ok(health);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/heart-rates")
    @Operation(summary = "Lista batimentos cardíacos", description = "Retorna os registros recentes de frequência cardíaca do dispositivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Batimentos cardíacos retornados com sucesso."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado ou sem dados.")
    })
    public ResponseEntity<List<HeartRate>> getHeartRate(
            @Parameter(description = "IMEI canônico do dispositivo cadastrado.", example = "123458800000015")
            @PathVariable("imei") String imei) {
        List<HeartRate> heartRate = healthService.getHeartRates(imei, SINCE);
        if (heartRate != null) {
            return ResponseEntity.ok(heartRate);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/body-temperatures")
    @Operation(summary = "Lista temperaturas corporais", description = "Retorna os registros recentes de temperatura corporal do dispositivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Temperaturas corporais retornadas com sucesso."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado ou sem dados.")
    })
    public ResponseEntity<List<BodyTemperature>> getBodyTemperatures(
            @Parameter(description = "IMEI canônico do dispositivo cadastrado.", example = "123458800000015")
            @PathVariable("imei") String imei) {
        List<BodyTemperature> heartRate = healthService.getBodyTemperatures(imei, SINCE);
        if (heartRate != null) {
            return ResponseEntity.ok(heartRate);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/blood-pressures")
    @Operation(summary = "Lista pressões arteriais", description = "Retorna os registros recentes de pressão arterial do dispositivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pressões arteriais retornadas com sucesso."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado ou sem dados.")
    })
    public ResponseEntity<List<BloodPressure>> getBloodPressures(
            @Parameter(description = "IMEI canônico do dispositivo cadastrado.", example = "123458800000015")
            @PathVariable("imei") String imei) {
        List<BloodPressure> heartRate = healthService.getBloodPressures(imei, SINCE);
        if (heartRate != null) {
            return ResponseEntity.ok(heartRate);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/sleep-sessions")
    @Operation(summary = "Lista sessões de sono", description = "Retorna as sessões recentes de sono registradas para o dispositivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessões de sono retornadas com sucesso."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado ou sem dados.")
    })
    public ResponseEntity<List<SleepSession>> getSleepSessions(
            @Parameter(description = "IMEI canônico do dispositivo cadastrado.", example = "123458800000015")
            @PathVariable("imei") String imei) {
        List<SleepSession> heartRate = healthService.getSleepSessions(imei, SINCE);
        if (heartRate != null) {
            return ResponseEntity.ok(heartRate);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/ecg-data")
    @Operation(summary = "Lista leituras de ECG", description = "Retorna os pacotes recentes de dados de ECG recebidos do dispositivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados de ECG retornados com sucesso."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado ou sem dados.")
    })
    public ResponseEntity<List<EcgData>> getEcgData(
            @Parameter(description = "IMEI canônico do dispositivo cadastrado.", example = "123458800000015")
            @PathVariable("imei") String imei) {
        List<EcgData> heartRate = healthService.getEcgData(imei, SINCE);
        if (heartRate != null) {
            return ResponseEntity.ok(heartRate);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/heartbeats")
    @Operation(summary = "Lista heartbeats", description = "Retorna os heartbeats recentes recebidos do dispositivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Heartbeats retornados com sucesso."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado ou sem dados.")
    })
    public ResponseEntity<List<Heartbeat>> getHeartbeats(
            @Parameter(description = "IMEI canônico do dispositivo cadastrado.", example = "123458800000015")
            @PathVariable("imei") String imei) {
        List<Heartbeat> heartRate = healthService.getHeartbeats(imei, 30L);
        if (heartRate != null) {
            return ResponseEntity.ok(heartRate);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/oxygen-levels")
    @Operation(summary = "Lista níveis de oxigênio", description = "Retorna os registros recentes de SpO2/oxigênio do dispositivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Níveis de oxigênio retornados com sucesso."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado ou sem dados.")
    })
    public ResponseEntity<List<Oxygen>> getOxygenLevels(
            @Parameter(description = "IMEI canônico do dispositivo cadastrado.", example = "123458800000015")
            @PathVariable("imei") String imei) {
        List<Oxygen> oxygenLevels = healthService.getOxygenLevels(imei, SINCE);
        if (oxygenLevels != null) {
            return ResponseEntity.ok(oxygenLevels);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/locations")
    @Operation(summary = "Lista localizações", description = "Retorna os registros recentes de localização do dispositivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Localizações retornadas com sucesso."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado ou sem dados.")
    })
    public ResponseEntity<List<Location>> getLocations(
            @Parameter(description = "IMEI canônico do dispositivo cadastrado.", example = "123458800000015")
            @PathVariable("imei") String imei) {
        List<Location> locations = healthService.getLocations(imei, SINCE);
        if (locations != null) {
            return ResponseEntity.ok(locations);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/sos-alerts")
    @Operation(summary = "Lista alertas SOS", description = "Retorna os alertas recentes SOS do dispositivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alertas SOS retornados com sucesso."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado ou sem dados.")
    })
    public ResponseEntity<List<Location>> getSOSAlerts(
            @Parameter(description = "IMEI canônico do dispositivo cadastrado.", example = "123458800000015")
            @PathVariable("imei") String imei) {
        List<Location> locations = healthService.getSOSAlerts(imei, SINCE);
        if (locations != null) {
            return ResponseEntity.ok(locations);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/fall-down-alerts")
    @Operation(summary = "Lista alertas de queda", description = "Retorna os alertas recentes de queda detectados pelo dispositivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alertas de queda retornados com sucesso."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado ou sem dados.")
    })
    public ResponseEntity<List<FallDownAlert>> getFallDownAlerts(
            @Parameter(description = "IMEI canônico do dispositivo cadastrado.", example = "123458800000015")
            @PathVariable("imei") String imei) {
        List<FallDownAlert> fallDownAlerts = healthService.getFallDownAlerts(imei, SINCE);
        if (fallDownAlerts != null) {
            return ResponseEntity.ok(fallDownAlerts);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/sleep-entries")
    @Operation(summary = "Lista entradas de sono", description = "Retorna os blocos recentes de sono associados às sessões de sono do dispositivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entradas de sono retornadas com sucesso."),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado ou sem dados.")
    })
    public ResponseEntity<List<SleepEntry>> getSleepEntries(
            @Parameter(description = "IMEI canônico do dispositivo cadastrado.", example = "123458800000015")
            @PathVariable("imei") String imei) {
        List<SleepEntry> sleepEntries = healthService.getSleepEntries(imei, SINCE);
        if (sleepEntries != null) {
            return ResponseEntity.ok(sleepEntries);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
