package com.researchoffice.orders.internal;

import com.researchoffice.files.FilesApi.FileContent;
import com.researchoffice.orders.internal.OrderDtos.ExpertNoteRequest;
import com.researchoffice.orders.internal.OrderDtos.ExpertTask;
import com.researchoffice.orders.internal.OrderDtos.ExpertTaskDetail;
import com.researchoffice.orders.internal.OrderDtos.SubmitRequest;
import com.researchoffice.shared.web.CurrentUser;
import com.researchoffice.shared.web.Downloads;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The expert's own tasks. Every call is limited to requests assigned to the signed-in expert. */
@RestController
@RequestMapping("/api/expert/tasks")
@RequiredArgsConstructor
class ExpertTaskController {

    private final OrderService service;

    @GetMapping
    List<ExpertTask> list(@AuthenticationPrincipal Jwt jwt) {
        return service.expertTasks(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    ExpertTaskDetail detail(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.expertTask(id, CurrentUser.id(jwt));
    }

    @PostMapping("/{id}/start")
    ExpertTaskDetail start(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.expertStart(id, CurrentUser.id(jwt), CurrentUser.name(jwt));
    }

    @PostMapping("/{id}/submit")
    ExpertTaskDetail submit(@PathVariable UUID id, @Valid @RequestBody SubmitRequest request,
                            @AuthenticationPrincipal Jwt jwt) {
        return service.expertSubmit(id, CurrentUser.id(jwt), request.note(), CurrentUser.name(jwt));
    }

    @PostMapping("/{id}/notes")
    ExpertTaskDetail note(@PathVariable UUID id, @Valid @RequestBody ExpertNoteRequest request,
                          @AuthenticationPrincipal Jwt jwt) {
        return service.expertNote(id, CurrentUser.id(jwt), request.note(), CurrentUser.name(jwt));
    }

    @PostMapping(path = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ExpertTaskDetail upload(@PathVariable UUID id, @RequestPart("files") List<MultipartFile> files,
                            @AuthenticationPrincipal Jwt jwt) {
        return service.expertUpload(id, CurrentUser.id(jwt), files, CurrentUser.name(jwt));
    }

    @GetMapping("/{id}/files/{fileId}")
    ResponseEntity<Resource> download(@PathVariable UUID id, @PathVariable UUID fileId, @AuthenticationPrincipal Jwt jwt) {
        FileContent f = service.expertDownload(id, CurrentUser.id(jwt), fileId);
        return Downloads.attachment(f.meta().name(), f.meta().contentType(), f.meta().sizeBytes(), f.stream());
    }
}
