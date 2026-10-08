package br.com.zontec.watchmonitor.server.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ação semântica do comando a ser enviado ao dispositivo.")
public enum DeviceCommandAction {
    RAW,
    LOCATE,
    SET_UPLOAD_INTERVAL,
    FALLDOWN,
    CALL,
    MONITOR,
    CENTER,
    APN,
    HEALTH_MONITOR,
    HEALTH_AUTO_SET,
    BODY_TEMPERATURE,
    SET_GMT_BRASILIA
}
