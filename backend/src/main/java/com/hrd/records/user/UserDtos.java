package com.hrd.records.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request/response payloads for user administration. */
public final class UserDtos {

    private UserDtos() {
    }

    public record UserSummary(Long id, String username, String fullName, Role role, boolean enabled) {

        public static UserSummary of(UserAccount user) {
            return new UserSummary(user.getId(), user.getUsername(), user.getFullName(),
                    user.getRole(), user.isEnabled());
        }
    }

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 100) String fullName,
            @NotNull Role role) {
    }

    public record UpdateUserRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotNull Role role,
            boolean enabled) {
    }
}
