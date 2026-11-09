package com.researchoffice.catalog.internal;

import com.researchoffice.catalog.internal.CatalogService.ServiceUpdate;
import com.researchoffice.catalog.internal.CatalogService.ServiceView;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class CatalogController {

    private final CatalogService catalog;

    @GetMapping("/api/public/services")
    List<ServiceView> publicServices() {
        return catalog.activeServices();
    }

    @GetMapping("/api/admin/services")
    List<ServiceView> allServices() {
        return catalog.allServices();
    }

    @PutMapping("/api/admin/services/{id}")
    ServiceView update(@PathVariable UUID id, @Valid @RequestBody ServiceUpdate update) {
        return catalog.update(id, update);
    }
}
