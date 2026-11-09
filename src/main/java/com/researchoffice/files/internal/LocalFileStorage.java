package com.researchoffice.files.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Stores files under {@code ro.files.dir} (RO_FILES_DIR). Keys are generated, never user input. */
@Component
class LocalFileStorage implements FileStorage {

    private final Path root;

    LocalFileStorage(FilesProperties props) throws IOException {
        this.root = Path.of(props.dir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public void put(String key, InputStream content) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public InputStream get(String key) throws IOException {
        return Files.newInputStream(resolve(key));
    }

    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return p;
    }

    @ConfigurationProperties("ro.files")
    record FilesProperties(String dir) {
        FilesProperties {
            dir = dir == null || dir.isBlank() ? "data/files" : dir;
        }
    }
}
