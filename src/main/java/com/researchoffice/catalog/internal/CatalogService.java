package com.researchoffice.catalog.internal;

import com.researchoffice.catalog.CatalogApi;
import com.researchoffice.shared.error.AppException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class CatalogService implements CatalogApi {

    private final ServiceOfferingRepository repository;

    record ServiceView(UUID id, String code, String category, String nameAr, String nameEn,
                       String descriptionAr, boolean active, int sortOrder) {
        static ServiceView of(ServiceOffering s) {
            return new ServiceView(s.getId(), s.getCode(), s.getCategory().name(), s.getNameAr(), s.getNameEn(),
                    s.getDescriptionAr(), s.isActive(), s.getSortOrder());
        }
    }

    record ServiceUpdate(@NotBlank @Size(max = 120) String nameAr, @NotBlank @Size(max = 120) String nameEn,
                         @NotBlank @Size(max = 600) String descriptionAr, boolean active, int sortOrder) {
    }

    List<ServiceView> activeServices() {
        return repository.findByActiveTrueOrderBySortOrder().stream().map(ServiceView::of).toList();
    }

    List<ServiceView> allServices() {
        return repository.findAllByOrderBySortOrder().stream().map(ServiceView::of).toList();
    }

    @Transactional
    ServiceView update(UUID id, ServiceUpdate u) {
        ServiceOffering s = repository.findById(id).orElseThrow(() -> AppException.notFound("Service", id));
        s.update(u.nameAr(), u.nameEn(), u.descriptionAr(), u.active(), u.sortOrder());
        return ServiceView.of(s);
    }

    @Override
    public Optional<ServiceRef> findActive(UUID serviceId) {
        return repository.findById(serviceId).filter(ServiceOffering::isActive).map(CatalogService::ref);
    }

    @Override
    public Optional<ServiceRef> find(UUID serviceId) {
        return repository.findById(serviceId).map(CatalogService::ref);
    }

    private static ServiceRef ref(ServiceOffering s) {
        return new ServiceRef(s.getId(), s.getCode(), s.getNameAr());
    }
}
