package com.hrd.records.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link JwtService}: round-trip of custom claims, expiry,
 * tampering and secret-strength validation. No Spring context involved.
 */
class JwtServiceTest {

    /** 64 chars = 512 bits, comfortably above the HS256 minimum of 256 bits. */
    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void generateThenParseReturnsSubjectRoleAndNameClaims() {
        JwtService service = new JwtService(SECRET, 5);
        String token = service.generateToken("jdoe", "ADMIN", "John Doe");

        Claims claims = service.parse(token);

        assertThat(claims.getSubject()).isEqualTo("jdoe");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.get("name", String.class)).isEqualTo("John Doe");
    }

    @Test
    void expiredTokenFailsToParse() {
        // negative ttl puts the expiration in the past, so parsing must reject it
        JwtService service = new JwtService(SECRET, -1);
        String token = service.generateToken("jdoe", "ADMIN", "John Doe");

        assertThatExceptionOfType(JwtException.class)
                .isThrownBy(() -> service.parse(token));
    }

    @Test
    void tamperedTokenFailsToParse() {
        JwtService service = new JwtService(SECRET, 5);
        String token = service.generateToken("jdoe", "ADMIN", "John Doe");

        String tampered = flipFirstSignatureChar(token);
        assertThat(tampered).isNotEqualTo(token);

        assertThatExceptionOfType(JwtException.class)
                .isThrownBy(() -> service.parse(tampered));
    }

    @Test
    void shortSecretIsRejectedAtConstruction() {
        assertThatExceptionOfType(WeakKeyException.class)
                .isThrownBy(() -> new JwtService("short-secret", 5));
    }

    /** Replaces the first character of the signature segment with a different valid base64 char. */
    private static String flipFirstSignatureChar(String token) {
        int signatureStart = token.lastIndexOf('.') + 1;
        char original = token.charAt(signatureStart);
        char replacement = original == 'A' ? 'B' : 'A';
        return token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
    }
}
