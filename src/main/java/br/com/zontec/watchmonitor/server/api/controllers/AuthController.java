package br.com.zontec.watchmonitor.server.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.zontec.watchmonitor.server.api.dto.AccessTokenRequest;
import br.com.zontec.watchmonitor.server.api.dto.AccessTokenResponse;
import br.com.zontec.watchmonitor.server.config.SecurityProperties;
import br.com.zontec.watchmonitor.server.service.AccessTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Operações para emissão de access_token.")
public class AuthController {

    private final SecurityProperties securityProperties;
    private final AccessTokenService accessTokenService;

    public AuthController(SecurityProperties securityProperties, AccessTokenService accessTokenService) {
        this.securityProperties = securityProperties;
        this.accessTokenService = accessTokenService;
    }

    @PostMapping("/token")
    @SecurityRequirements
    @Operation(
            summary = "Cria um access_token",
            description = "Valida as credenciais configuradas da API e retorna um token Bearer para acessar os endpoints protegidos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token emitido com sucesso."),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas.")
    })
    public ResponseEntity<AccessTokenResponse> createAccessToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Credenciais da API.",
                    content = @Content(
                            schema = @Schema(implementation = AccessTokenRequest.class),
                            examples = @ExampleObject(value = "{\"username\":\"admin\",\"password\":\"admin\"}")
                    )
            )
            @RequestBody AccessTokenRequest request) {
        if (!validCredentials(request)) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = accessTokenService.createAccessToken(request.username().trim());
        return ResponseEntity.ok(new AccessTokenResponse(accessToken, "Bearer", accessTokenService.expiresInSeconds()));
    }

    private boolean validCredentials(AccessTokenRequest request) {
        return request != null
                && request.username() != null
                && request.password() != null
                && securityProperties.username().equals(request.username().trim())
                && securityProperties.password().equals(request.password());
    }
}
