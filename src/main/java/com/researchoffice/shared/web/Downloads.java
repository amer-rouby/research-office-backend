package com.researchoffice.shared.web;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** One way to stream a stored file to the browser (UTF-8 safe file names, Arabic included). */
public final class Downloads {

    private Downloads() {
    }

    public static ResponseEntity<Resource> attachment(String name, String contentType, long size, InputStream stream) {
        var disposition = ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(size)
                .body(new InputStreamResource(stream));
    }
}
