package br.com.zontec.watchmonitor.server.api.dto;

import java.time.LocalDateTime;

public record AudioMessageResponse(
        Long id,
        String imei,
        String fileName,
        LocalDateTime timestamp,
        String direction,
        Boolean sended,
        String streamUrl
) {
}
