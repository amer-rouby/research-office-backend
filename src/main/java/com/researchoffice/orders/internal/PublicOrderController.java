package com.researchoffice.orders.internal;

import com.researchoffice.files.FilesApi.FileContent;
import com.researchoffice.orders.internal.OrderDtos.CreateOrderRequest;
import com.researchoffice.orders.internal.OrderDtos.CreatedOrder;
import com.researchoffice.orders.internal.OrderDtos.FileView;
import com.researchoffice.orders.internal.OrderDtos.RevisionRequest;
import com.researchoffice.orders.internal.OrderDtos.TrackingView;
import com.researchoffice.shared.web.Downloads;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Client side: no login, the unguessable tracking token in the link is the key. */
@RestController
@RequestMapping("/api/public/orders")
@RequiredArgsConstructor
class PublicOrderController {

    private final OrderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreatedOrder create(@Valid @RequestBody CreateOrderRequest request) {
        return service.create(request);
    }

    @GetMapping("/{token}")
    TrackingView track(@PathVariable String token) {
        return service.track(token);
    }

    @PostMapping(path = "/{token}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    List<FileView> upload(@PathVariable String token, @RequestPart("files") List<MultipartFile> files) {
        return service.clientUpload(token, files);
    }

    @PostMapping("/{token}/accept-quote")
    TrackingView acceptQuote(@PathVariable String token) {
        return service.acceptQuote(token);
    }

    @PostMapping("/{token}/revision")
    TrackingView revision(@PathVariable String token, @Valid @RequestBody RevisionRequest request) {
        return service.requestRevision(token, request.note());
    }

    @PostMapping("/{token}/confirm")
    TrackingView confirm(@PathVariable String token) {
        return service.confirmDelivery(token);
    }

    @GetMapping("/{token}/files/{fileId}")
    ResponseEntity<Resource> download(@PathVariable String token, @PathVariable UUID fileId) {
        FileContent f = service.clientDownload(token, fileId);
        return Downloads.attachment(f.meta().name(), f.meta().contentType(), f.meta().sizeBytes(), f.stream());
    }
}
