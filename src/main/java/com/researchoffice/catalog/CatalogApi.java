package com.researchoffice.catalog;

import java.util.Optional;
import java.util.UUID;

/** What other modules may ask the catalog. */
public interface CatalogApi {

    Optional<ServiceRef> findActive(UUID serviceId);

    Optional<ServiceRef> find(UUID serviceId);

    record ServiceRef(UUID id, String code, String nameAr) {
    }
}
