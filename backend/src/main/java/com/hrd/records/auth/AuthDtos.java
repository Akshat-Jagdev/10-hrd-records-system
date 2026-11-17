package com.hrd.records.auth;

import com.hrd.records.user.UserDtos;
import jakarta.validation.constraints.NotBlank;

/** Request/response payloads for authentication. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String token, String tokenType, long expiresIn, UserDtos.UserSummary user) {
    }
}
