package br.com.zontec.watchmonitor.server.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token de acesso emitido para consumo da API.")
public record AccessTokenResponse(
        @Schema(description = "Token JWT assinado que deve ser enviado no header Authorization.", example = "eyJhbGciOiJIUzI1NiJ9...")
        @JsonProperty("access_token")
        String accessToken,
        @Schema(description = "Tipo do token.", example = "Bearer")
        @JsonProperty("token_type")
        String tokenType,
        @Schema(description = "Tempo de validade em segundos.", example = "3600")
        @JsonProperty("expires_in")
        long expiresIn
) {
}
