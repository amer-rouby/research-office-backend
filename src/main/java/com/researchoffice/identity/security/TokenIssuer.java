package com.researchoffice.identity.security;

import com.researchoffice.identity.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Issues short-lived HS256 access tokens carrying the user's id, name and roles. */
@Component
public class TokenIssuer {

    static final String ISSUER = "research-office";
    static final String ROLES_CLAIM = "roles";

    private final JwtEncoder encoder;
    private final SecurityProperties props;
    private final Clock clock;

    TokenIssuer(JwtEncoder encoder, SecurityProperties props, Clock clock) {
        this.encoder = encoder;
        this.props = props;
        this.clock = clock;
    }

    public AccessToken accessToken(UUID userId, String email, String fullName, Role role) {
        Instant now = clock.instant();
        var claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(props.accessTokenTtl()))
                .claim("email", email)
                .claim("name", fullName)
                .claim(ROLES_CLAIM, List.of(role.name()))
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, props.accessTokenTtl().toSeconds());
    }

    public Duration refreshTtl() {
        return props.refreshTokenTtl();
    }

    public record AccessToken(String value, long expiresInSeconds) {
    }
}
