package com.researchoffice.identity;

/** Staff roles. Clients never log in: they use a private tracking link. */
public enum Role {
    ADMIN,
    EXPERT;

    public String authority() {
        return "ROLE_" + name();
    }
}
