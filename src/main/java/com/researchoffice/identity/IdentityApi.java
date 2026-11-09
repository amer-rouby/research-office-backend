package com.researchoffice.identity;

import java.util.Optional;
import java.util.UUID;

/** What other modules may ask about staff members. */
public interface IdentityApi {

    Optional<StaffRef> findStaff(UUID userId);

    record StaffRef(UUID id, String fullName, Role role, boolean active) {
    }
}
