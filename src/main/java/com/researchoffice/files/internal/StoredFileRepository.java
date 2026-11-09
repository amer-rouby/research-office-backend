package com.researchoffice.files.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

    List<StoredFile> findByOwnerIdOrderByCreatedAt(UUID ownerId);

    Optional<StoredFile> findByIdAndOwnerId(UUID id, UUID ownerId);
}
