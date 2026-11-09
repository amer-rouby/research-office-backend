package com.researchoffice.orders.internal;

import com.researchoffice.files.FilesApi.FileContent;
import com.researchoffice.files.FilesApi.FileKind;
import com.researchoffice.orders.OrderStatus;
import com.researchoffice.orders.internal.OrderDtos.NoteRequest;
import com.researchoffice.orders.internal.OrderDtos.OrderDetail;
import com.researchoffice.orders.internal.OrderDtos.OrderStats;
import com.researchoffice.orders.internal.OrderDtos.OrderSummary;
import com.researchoffice.orders.internal.OrderDtos.QuoteRequest;
import com.researchoffice.orders.internal.OrderDtos.StatusChangeRequest;
import com.researchoffice.orders.internal.OrderDtos.AssignRequest;
import com.researchoffice.orders.internal.OrderDtos.PaymentRequest;
import com.researchoffice.shared.web.CurrentUser;
import com.researchoffice.shared.web.Downloads;
import com.researchoffice.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
class AdminOrderController {

    private final OrderService service;

    @GetMapping
    PageResponse<OrderSummary> list(@RequestParam(required = false) OrderStatus status,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return service.list(status, q, pageable);
    }

    @GetMapping("/stats")
    OrderStats stats() {
        return service.stats();
    }

    @GetMapping("/{id}")
    OrderDetail detail(@PathVariable UUID id) {
        return service.detail(id);
    }

    @PostMapping("/{id}/quote")
    OrderDetail quote(@PathVariable UUID id, @Valid @RequestBody QuoteRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.quote(id, request, actor(jwt));
    }

    @PostMapping("/{id}/status")
    OrderDetail status(@PathVariable UUID id, @Valid @RequestBody StatusChangeRequest request,
                       @AuthenticationPrincipal Jwt jwt) {
        return service.changeStatus(id, request, actor(jwt));
    }

    @PostMapping("/{id}/notes")
    OrderDetail note(@PathVariable UUID id, @Valid @RequestBody NoteRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.addNote(id, request, actor(jwt));
    }

    @PostMapping(path = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    OrderDetail upload(@PathVariable UUID id, @RequestParam FileKind kind,
                       @RequestPart("files") List<MultipartFile> files, @AuthenticationPrincipal Jwt jwt) {
        return service.staffUpload(id, kind, files, actor(jwt));
    }

    @GetMapping("/{id}/files/{fileId}")
    ResponseEntity<Resource> download(@PathVariable UUID id, @PathVariable UUID fileId) {
        FileContent f = service.staffDownload(id, fileId);
        return Downloads.attachment(f.meta().name(), f.meta().contentType(), f.meta().sizeBytes(), f.stream());
    }

    @PostMapping("/{id}/assign")
    OrderDetail assign(@PathVariable UUID id, @Valid @RequestBody AssignRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.assign(id, request.expertId(), actor(jwt));
    }

    @PostMapping("/{id}/files/{fileId}/publish")
    OrderDetail publish(@PathVariable UUID id, @PathVariable UUID fileId, @AuthenticationPrincipal Jwt jwt) {
        return service.publishFile(id, fileId, actor(jwt));
    }

    @PostMapping("/{id}/payments")
    OrderDetail recordPayment(@PathVariable UUID id, @Valid @RequestBody PaymentRequest request,
                              @AuthenticationPrincipal Jwt jwt) {
        return service.recordPayment(id, request, actor(jwt));
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    OrderDetail deletePayment(@PathVariable UUID id, @PathVariable UUID paymentId, @AuthenticationPrincipal Jwt jwt) {
        return service.deletePayment(id, paymentId, actor(jwt));
    }

    private static String actor(Jwt jwt) {
        return CurrentUser.name(jwt);
    }
}
