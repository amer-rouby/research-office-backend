package com.researchoffice.files;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/** Stores files for any owner (e.g. an order). Callers decide who may see which {@link FileKind}. */
public interface FilesApi {

    FileRef store(UUID ownerId, FileKind kind, MultipartFile file, String uploadedBy);

    List<FileRef> list(UUID ownerId);

    /** Opens a file only if it belongs to the given owner. */
    FileContent open(UUID ownerId, UUID fileId);

    /** Makes a reviewed internal file (e.g. an expert's work) visible to the client as a deliverable. */
    FileRef publish(UUID ownerId, UUID fileId);

    enum FileKind {
        /** Sent by the client with the request. */
        CLIENT_UPLOAD,
        /** Work delivered to the client. */
        DELIVERABLE,
        /** Staff-only working file. */
        INTERNAL;

        public boolean visibleToClient() {
            return this != INTERNAL;
        }
    }

    record FileRef(UUID id, FileKind kind, String name, String contentType, long sizeBytes,
                   String uploadedBy, Instant uploadedAt) {
    }

    record FileContent(FileRef meta, InputStream stream) {
    }
}
