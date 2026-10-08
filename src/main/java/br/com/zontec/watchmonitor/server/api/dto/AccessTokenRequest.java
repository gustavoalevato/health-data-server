package br.com.zontec.watchmonitor.server.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credenciais para emissão de access_token.")
public record AccessTokenRequest(
        @Schema(description = "Usuário configurado para acesso à API.", example = "admin")
        String username,
        @Schema(description = "Senha configurada para acesso à API.", example = "admin")
        String password
) {
}
