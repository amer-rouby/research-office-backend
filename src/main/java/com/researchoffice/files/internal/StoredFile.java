package com.researchoffice.files.internal;

import com.researchoffice.files.FilesApi.FileKind;
import com.researchoffice.files.FilesApi.FileRef;
import com.researchoffice.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "stored_file")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class StoredFile extends BaseEntity {

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileKind kind;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    StoredFile(UUID ownerId, FileKind kind, String originalName, String contentType, long sizeBytes,
               String storageKey, String uploadedBy) {
        this.ownerId = ownerId;
        this.kind = kind;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.uploadedBy = uploadedBy;
    }

    void publish() {
        if (kind != FileKind.INTERNAL) {
            throw com.researchoffice.shared.error.AppException.rule("FILE_NOT_INTERNAL", "Only internal files can be published");
        }
        kind = FileKind.DELIVERABLE;
    }

    FileRef toRef() {
        return new FileRef(getId(), kind, originalName, contentType, sizeBytes, uploadedBy, getCreatedAt());
    }
}
