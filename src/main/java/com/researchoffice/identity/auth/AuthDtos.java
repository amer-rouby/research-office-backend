package com.researchoffice.identity.auth;

import com.researchoffice.identity.Role;
import com.researchoffice.identity.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/** Request/response shapes of the auth API (exposed through OpenAPI to the web client). */
final class AuthDtos {

    private AuthDtos() {
    }

    record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    record RefreshRequest(@NotBlank String refreshToken) {
    }

    record UserResponse(UUID id, String email, String fullName, Role role) {
        static UserResponse of(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole());
        }
    }

    record TokenResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
    }
}
