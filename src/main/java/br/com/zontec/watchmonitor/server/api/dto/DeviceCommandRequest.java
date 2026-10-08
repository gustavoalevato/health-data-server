package br.com.zontec.watchmonitor.server.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DeviceCommandRequest(
        @Schema(description = "Tipo da ação semântica a ser executada.", example = "SET_UPLOAD_INTERVAL")
        DeviceCommandAction action,
        @Schema(description = "Payload bruto para ação RAW. Exemplo GPSWatch: `BP16`. Exemplo 4PTouch: `CR`.", example = "BP16")
        String payload,
        @Schema(description = "Intervalo de upload em segundos, usado em SET_UPLOAD_INTERVAL.", example = "600")
        Integer intervalSeconds,
        @Schema(description = "Telefone usado nas ações CALL e MONITOR.", example = "11999999999")
        String phoneNumber,
        @Schema(description = "Número central usado na ação CENTER.", example = "11999999999")
        String centerNumber,
        @Schema(description = "Nome do APN usado na ação APN.", example = "cmnet")
        String apnName,
        @Schema(description = "Usuário do APN usado na ação APN.", example = "")
        String apnUser,
        @Schema(description = "Senha do APN usada na ação APN.", example = "")
        String apnPassword,
        @Schema(description = "Código MCCMNC usado na ação APN.", example = "72405")
        String mccmncCode,
        @Schema(description = "Habilitar alarme de queda usado na ação FALLDOWN.", example = "true")
        Boolean fallDownEnabled,
        @Schema(description = "Habilitar alarme de queda por SMS usado na ação FALLDOWN.", example = "true")
        Boolean fallDownAlarmEnabled,
        @Schema(description = "Intervalo de upload em segundos, usado em hrtstart para monitoramento de saúde.", example = "1800")
        Integer healthMonitorInterval,
        @Schema(description = "Intervalo de upload em minutos, usado em healthautoset para monitoramento automatico de saúde.", example = "180")
        Integer healthAutosetInterval
) {

    public DeviceCommandAction normalizedAction() {
        return action == null ? DeviceCommandAction.RAW : action;
    }
}
