package com.researchoffice.shared.web;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the signed-in staff member from the access token (claims set by the identity module). */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    /** Display name for timelines; falls back to the user id. */
    public static String name(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        return name == null || name.isBlank() ? jwt.getSubject() : name;
    }
}
