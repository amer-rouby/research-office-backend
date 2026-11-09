package com.researchoffice.catalog.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {

    List<ServiceOffering> findByActiveTrueOrderBySortOrder();

    List<ServiceOffering> findAllByOrderBySortOrder();
}
