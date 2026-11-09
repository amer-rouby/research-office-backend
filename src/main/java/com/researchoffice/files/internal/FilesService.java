package com.researchoffice.files.internal;

import com.researchoffice.files.FilesApi;
import com.researchoffice.shared.error.AppException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
class FilesService implements FilesApi {

    /** Documents, data and images used in research work. Executables and scripts are refused. */
    private static final Set<String> ALLOWED = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "csv", "ppt", "pptx", "txt", "rtf", "odt", "ods",
            "sav", "spv", "omv", "dta", "sps", "zip", "rar", "7z", "png", "jpg", "jpeg", "webp");
    private static final long MAX_BYTES = 50L * 1024 * 1024;

    private final StoredFileRepository repository;
    private final FileStorage storage;

    @Override
    @Transactional
    public FileRef store(UUID ownerId, FileKind kind, MultipartFile file, String uploadedBy) {
        String name = cleanName(file.getOriginalFilename());
        String ext = extension(name);
        if (file.isEmpty()) {
            throw AppException.rule("FILE_EMPTY", "The file is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw AppException.rule("FILE_TOO_LARGE", "Files must be 50 MB or smaller");
        }
        if (!ALLOWED.contains(ext)) {
            throw AppException.rule("FILE_TYPE_NOT_ALLOWED", "File type ." + ext + " is not allowed");
        }
        String key = ownerId + "/" + UUID.randomUUID() + "." + ext;
        try (InputStream in = file.getInputStream()) {
            storage.put(key, in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store file", e);
        }
        String type = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        return repository.save(new StoredFile(ownerId, kind, name, type, file.getSize(), key, uploadedBy)).toRef();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileRef> list(UUID ownerId) {
        return repository.findByOwnerIdOrderByCreatedAt(ownerId).stream().map(StoredFile::toRef).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FileContent open(UUID ownerId, UUID fileId) {
        StoredFile f = repository.findByIdAndOwnerId(fileId, ownerId)
                .orElseThrow(() -> AppException.notFound("File", fileId));
        try {
            return new FileContent(f.toRef(), storage.get(f.getStorageKey()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read file", e);
        }
    }

    private static String cleanName(String original) {
        String n = original == null ? "file" : original.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        return n.isEmpty() ? "file" : (n.length() > 200 ? n.substring(n.length() - 200) : n);
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
