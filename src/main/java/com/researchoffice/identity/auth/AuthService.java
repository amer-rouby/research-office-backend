package com.researchoffice.identity.auth;

import com.researchoffice.identity.auth.AuthDtos.TokenResponse;
import com.researchoffice.identity.auth.AuthDtos.UserResponse;
import com.researchoffice.identity.security.TokenIssuer;
import com.researchoffice.identity.user.User;
import com.researchoffice.identity.user.UserRepository;
import com.researchoffice.shared.error.AppException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;
    private final Clock clock;

    @Transactional
    public TokenResponse login(String email, String password) {
        User user = users.findByEmail(email.trim())
                .filter(User::isActive)
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(AuthService::badCredentials);
        return issue(user);
    }

    /** Rotates the refresh token. Re-use of a revoked token revokes the whole family (likely theft). */
    @Transactional(noRollbackFor = AppException.class)
    public TokenResponse refresh(String rawToken) {
        RefreshToken token = refreshTokens.findByTokenHash(hash(rawToken)).orElseThrow(AuthService::badToken);
        if (token.isRevoked()) {
            refreshTokens.revokeAllForUser(token.getUserId());
            throw badToken();
        }
        if (!token.usableAt(clock.instant())) {
            throw badToken();
        }
        token.revoke();
        User user = users.findById(token.getUserId()).filter(User::isActive).orElseThrow(AuthService::badToken);
        return issue(user);
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(hash(rawToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional(readOnly = true)
    public UserResponse me(UUID userId) {
        return users.findById(userId).map(UserResponse::of).orElseThrow(() -> AppException.notFound("User", userId));
    }

    private TokenResponse issue(User user) {
        var access = tokenIssuer.accessToken(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires = clock.instant().plus(tokenIssuer.refreshTtl());
        refreshTokens.save(new RefreshToken(user.getId(), hash(raw), expires));
        return new TokenResponse(access.value(), raw, access.expiresInSeconds(), UserResponse.of(user));
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static AppException badCredentials() {
        return new AppException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid email or password");
    }

    private static AppException badToken() {
        return new AppException(HttpStatus.UNAUTHORIZED, "BAD_REFRESH_TOKEN", "Session expired, please sign in again");
    }
}
