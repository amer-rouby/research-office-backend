package com.researchoffice.orders.internal;

import com.researchoffice.files.FilesApi.FileKind;
import com.researchoffice.files.FilesApi.FileRef;
import com.researchoffice.orders.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Request/response shapes of the orders API (exposed through OpenAPI to the web client). */
final class OrderDtos {

    private OrderDtos() {
    }

    // ---- public (client) ----

    record CreateOrderRequest(
            @NotNull UUID serviceId,
            @NotBlank @Size(max = 150) String clientName,
            @NotBlank @Pattern(regexp = "^\\+?[0-9 ]{8,20}$", message = "invalid phone number") String clientPhone,
            @Schema(nullable = true) @Email @Size(max = 190) String clientEmail,
            @Schema(nullable = true) @Size(max = 60) String country,
            @NotBlank @Size(min = 10, max = 4000) String details,
            @Schema(nullable = true) @FutureOrPresent LocalDate deadline) {
    }

    record CreatedOrder(String reference, String trackingToken) {
    }

    record RevisionRequest(@NotBlank @Size(max = 2000) String note) {
    }

    record TrackingView(String reference, String serviceName, OrderStatus status, String clientName,
                        @Schema(nullable = true) LocalDate deadline,
                        @Schema(nullable = true) Quote quote,
                        List<TimelineEntry> timeline, List<FileView> files, Instant createdAt) {
    }

    // ---- staff (admin) ----

    record OrderSummary(UUID id, String reference, String serviceName, OrderStatus status, String clientName,
                        String clientPhone, @Schema(nullable = true) LocalDate deadline,
                        @Schema(nullable = true) Quote quote, Instant createdAt) {
    }

    record OrderDetail(UUID id, String reference, String trackingUrl, UUID serviceId, String serviceName,
                       OrderStatus status, Set<OrderStatus> nextStatuses, String clientName, String clientPhone,
                       @Schema(nullable = true) String clientEmail, @Schema(nullable = true) String country,
                       String details, @Schema(nullable = true) LocalDate deadline,
                       @Schema(nullable = true) Quote quote,
                       @Schema(nullable = true) ExpertRef assignedExpert,
                       List<TimelineEntry> timeline, List<FileView> files, Instant createdAt) {
    }

    record ExpertRef(UUID id, String fullName) {
    }

    record AssignRequest(@NotNull UUID expertId) {
    }

    // ---- expert ----

    /** An expert's view of a task: the work, never the client's contact details. */
    record ExpertTask(UUID id, String reference, String serviceName, OrderStatus status,
                      @Schema(nullable = true) LocalDate deadline, Instant updatedAt) {
    }

    record ExpertTaskDetail(UUID id, String reference, String serviceName, OrderStatus status, String details,
                            @Schema(nullable = true) LocalDate deadline, boolean canStart, boolean canSubmit,
                            List<TimelineEntry> timeline, List<FileView> files) {
    }

    record ExpertNoteRequest(@NotBlank @Size(max = 2000) String note) {
    }

    record SubmitRequest(@Schema(nullable = true) @Size(max = 2000) String note) {
    }

    record QuoteRequest(@NotNull @DecimalMin(value = "1.00") BigDecimal price,
                        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String currency,
                        @Schema(nullable = true) @Size(max = 1000) String note) {
    }

    record StatusChangeRequest(@NotNull OrderStatus status, @Schema(nullable = true) @Size(max = 2000) String note,
                               boolean visibleToClient) {
    }

    record NoteRequest(@NotBlank @Size(max = 2000) String note, boolean visibleToClient) {
    }

    record OrderStats(Map<OrderStatus, Long> byStatus, long open, long total) {
    }

    // ---- shared ----

    record Quote(BigDecimal price, String currency, @Schema(nullable = true) String note) {
        static Quote of(ServiceOrder o) {
            return o.getQuotedPrice() == null ? null : new Quote(o.getQuotedPrice(), o.getCurrency(), o.getQuoteNote());
        }
    }

    record TimelineEntry(UUID id, String type, @Schema(nullable = true) OrderStatus fromStatus,
                         @Schema(nullable = true) OrderStatus toStatus, @Schema(nullable = true) String note,
                         String actor, boolean visibleToClient, Instant at) {
        static TimelineEntry of(OrderEvent e) {
            return new TimelineEntry(e.getId(), e.getType().name(), e.getFromStatus(), e.getToStatus(), e.getNote(),
                    e.getActor(), e.isVisibleToClient(), e.getCreatedAt());
        }
    }

    record FileView(UUID id, FileKind kind, String name, String contentType, long sizeBytes, String uploadedBy,
                    Instant uploadedAt) {
        static FileView of(FileRef f) {
            return new FileView(f.id(), f.kind(), f.name(), f.contentType(), f.sizeBytes(), f.uploadedBy(),
                    f.uploadedAt());
        }
    }
}
