package br.com.zontec.watchmonitor.server.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.zontec.watchmonitor.server.config.SecurityProperties;

@Service
public class AccessTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_TYPE = "JWT";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    public AccessTokenService(SecurityProperties securityProperties, ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    public String createAccessToken(String username) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expiresInSeconds());

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", TOKEN_TYPE);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", username);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public String validateAndGetSubject(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token ausente.");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Token inválido.");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new IllegalArgumentException("Assinatura do token inválida.");
        }

        Map<String, Object> payload = decodeJson(parts[1]);
        long expiration = asLong(payload.get("exp"));
        if (Instant.now().getEpochSecond() >= expiration) {
            throw new IllegalArgumentException("Token expirado.");
        }

        Object subject = payload.get("sub");
        if (subject == null || subject.toString().isBlank()) {
            throw new IllegalArgumentException("Token sem usuário.");
        }

        return subject.toString();
    }

    public long expiresInSeconds() {
        return securityProperties.accessTokenExpirationMinutes() * 60;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return base64Url(objectMapper.writeValueAsBytes(value));
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o token.", e);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(value);
            return objectMapper.readValue(decoded, MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload do token inválido.", e);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(securityProperties.jwtSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return base64Url(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível assinar o token.", e);
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return java.security.MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
