package com.researchoffice.orders.internal;

import com.researchoffice.catalog.CatalogApi;
import com.researchoffice.catalog.CatalogApi.ServiceRef;
import com.researchoffice.files.FilesApi;
import com.researchoffice.files.FilesApi.FileContent;
import com.researchoffice.files.FilesApi.FileKind;
import com.researchoffice.orders.OrderStatus;
import com.researchoffice.orders.internal.OrderDtos.CreateOrderRequest;
import com.researchoffice.orders.internal.OrderDtos.CreatedOrder;
import com.researchoffice.orders.internal.OrderDtos.FileView;
import com.researchoffice.orders.internal.OrderDtos.NoteRequest;
import com.researchoffice.orders.internal.OrderDtos.OrderDetail;
import com.researchoffice.orders.internal.OrderDtos.OrderStats;
import com.researchoffice.orders.internal.OrderDtos.OrderSummary;
import com.researchoffice.orders.internal.OrderDtos.Quote;
import com.researchoffice.orders.internal.OrderDtos.QuoteRequest;
import com.researchoffice.orders.internal.OrderDtos.StatusChangeRequest;
import com.researchoffice.orders.internal.OrderDtos.TimelineEntry;
import com.researchoffice.orders.internal.OrderDtos.TrackingView;
import com.researchoffice.orders.internal.OrderEvent.Type;
import com.researchoffice.shared.error.AppException;
import com.researchoffice.shared.web.PageResponse;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Year;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
class OrderService {

    static final String CLIENT = "العميل";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orders;
    private final OrderEventRepository events;
    private final CatalogApi catalog;
    private final FilesApi files;
    private final OrdersProperties props;
    private final Clock clock;

    // ---------------- client (by tracking token) ----------------

    CreatedOrder create(CreateOrderRequest r) {
        ServiceRef service = catalog.findActive(r.serviceId())
                .orElseThrow(() -> AppException.rule("SERVICE_UNAVAILABLE", "This service is not available"));
        String reference = "RO-%d-%04d".formatted(Year.now(clock).getValue(), orders.nextReferenceNumber());
        var order = orders.save(new ServiceOrder(reference, newToken(), service.id(), r.clientName(), r.clientPhone(),
                r.clientEmail(), r.country(), r.details(), r.deadline()));
        log(order, Type.CREATED, null, OrderStatus.NEW, null, CLIENT, true);
        return new CreatedOrder(order.getReference(), order.getTrackingToken());
    }

    @Transactional(readOnly = true)
    TrackingView track(String token) {
        ServiceOrder o = byToken(token);
        return new TrackingView(o.getReference(), serviceName(o), o.getStatus(), o.getClientName(), o.getDeadline(),
                Quote.of(o), timeline(o, TimelineEntry::visibleToClient),
                fileViews(o, f -> f.kind().visibleToClient()), o.getCreatedAt());
    }

    List<FileView> clientUpload(String token, List<MultipartFile> uploads) {
        ServiceOrder o = byToken(token);
        requireOpen(o);
        return store(o, FileKind.CLIENT_UPLOAD, uploads, CLIENT);
    }

    TrackingView acceptQuote(String token) {
        ServiceOrder o = byToken(token);
        move(o, OrderStatus.ACCEPTED, "تم قبول عرض السعر", CLIENT, true);
        return track(token);
    }

    TrackingView requestRevision(String token, String note) {
        ServiceOrder o = byToken(token);
        move(o, OrderStatus.REVISION, note, CLIENT, true);
        return track(token);
    }

    TrackingView confirmDelivery(String token) {
        ServiceOrder o = byToken(token);
        move(o, OrderStatus.CLOSED, "تم تأكيد الاستلام", CLIENT, true);
        return track(token);
    }

    @Transactional(readOnly = true)
    FileContent clientDownload(String token, UUID fileId) {
        ServiceOrder o = byToken(token);
        FileContent content = files.open(o.getId(), fileId);
        if (!content.meta().kind().visibleToClient()) {
            throw AppException.notFound("File", fileId);
        }
        return content;
    }

    // ---------------- staff ----------------

    @Transactional(readOnly = true)
    PageResponse<OrderSummary> list(OrderStatus status, String query, Pageable pageable) {
        return PageResponse.of(orders.findAll(OrderRepository.matching(status, query), pageable),
                o -> new OrderSummary(o.getId(), o.getReference(), serviceName(o), o.getStatus(), o.getClientName(),
                        o.getClientPhone(), o.getDeadline(), Quote.of(o), o.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    OrderDetail detail(UUID id) {
        ServiceOrder o = byId(id);
        return new OrderDetail(o.getId(), o.getReference(), props.webBaseUrl() + "/t/" + o.getTrackingToken(),
                o.getServiceId(), serviceName(o), o.getStatus(), o.getStatus().next(), o.getClientName(),
                o.getClientPhone(), o.getClientEmail(), o.getCountry(), o.getDetails(), o.getDeadline(), Quote.of(o),
                timeline(o, e -> true), fileViews(o, f -> true), o.getCreatedAt());
    }

    OrderDetail quote(UUID id, QuoteRequest r, String actor) {
        ServiceOrder o = byId(id);
        OrderStatus from = o.quote(r.price(), r.currency(), r.note());
        log(o, Type.QUOTED, from, OrderStatus.QUOTED, "%s %s".formatted(r.price().toPlainString(),
                r.currency().toUpperCase()) + (r.note() == null || r.note().isBlank() ? "" : " — " + r.note()),
                actor, true);
        return detail(id);
    }

    OrderDetail changeStatus(UUID id, StatusChangeRequest r, String actor) {
        if (r.status() == OrderStatus.QUOTED) {
            throw AppException.rule("USE_QUOTE", "Send a quote to move a request to QUOTED");
        }
        move(byId(id), r.status(), r.note(), actor, r.visibleToClient());
        return detail(id);
    }

    OrderDetail addNote(UUID id, NoteRequest r, String actor) {
        ServiceOrder o = byId(id);
        log(o, Type.NOTE, null, null, r.note(), actor, r.visibleToClient());
        return detail(id);
    }

    OrderDetail staffUpload(UUID id, FileKind kind, List<MultipartFile> uploads, String actor) {
        if (kind == FileKind.CLIENT_UPLOAD) {
            throw AppException.rule("INVALID_FILE_KIND", "Staff upload deliverables or internal files");
        }
        store(byId(id), kind, uploads, actor);
        return detail(id);
    }

    @Transactional(readOnly = true)
    FileContent staffDownload(UUID id, UUID fileId) {
        return files.open(byId(id).getId(), fileId);
    }

    @Transactional(readOnly = true)
    OrderStats stats() {
        Map<OrderStatus, Long> by = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) {
            by.put(s, 0L);
        }
        orders.countByStatus().forEach(c -> by.put(c.getStatus(), c.getTotal()));
        long open = by.entrySet().stream().filter(e -> e.getKey().isOpen()).mapToLong(Map.Entry::getValue).sum();
        long total = by.values().stream().mapToLong(Long::longValue).sum();
        return new OrderStats(by, open, total);
    }

    // ---------------- helpers ----------------

    private void move(ServiceOrder o, OrderStatus to, String note, String actor, boolean visible) {
        OrderStatus from = o.moveTo(to);
        log(o, Type.STATUS_CHANGED, from, to, note, actor, visible);
    }

    private List<FileView> store(ServiceOrder o, FileKind kind, List<MultipartFile> uploads, String actor) {
        if (uploads == null || uploads.isEmpty()) {
            throw AppException.rule("NO_FILES", "Attach at least one file");
        }
        List<FileView> stored = uploads.stream().map(f -> FileView.of(files.store(o.getId(), kind, f, actor))).toList();
        String names = String.join("، ", stored.stream().map(FileView::name).toList());
        log(o, Type.FILE_ADDED, null, null, names, actor, kind.visibleToClient());
        return stored;
    }

    private void log(ServiceOrder o, Type type, OrderStatus from, OrderStatus to, String note, String actor,
                     boolean visible) {
        events.save(new OrderEvent(o.getId(), type, from, to, note, actor, visible));
    }

    private List<TimelineEntry> timeline(ServiceOrder o, Predicate<TimelineEntry> filter) {
        return events.findByOrderIdOrderByCreatedAt(o.getId()).stream().map(TimelineEntry::of).filter(filter).toList();
    }

    private List<FileView> fileViews(ServiceOrder o, Predicate<FilesApi.FileRef> filter) {
        return files.list(o.getId()).stream().filter(filter).map(FileView::of).toList();
    }

    private String serviceName(ServiceOrder o) {
        return catalog.find(o.getServiceId()).map(ServiceRef::nameAr).orElse("—");
    }

    private static void requireOpen(ServiceOrder o) {
        if (!o.getStatus().isOpen()) {
            throw AppException.rule("ORDER_CLOSED", "This request is closed");
        }
    }

    private ServiceOrder byToken(String token) {
        return orders.findByTrackingToken(token).orElseThrow(() -> AppException.notFound("Request", "link"));
    }

    private ServiceOrder byId(UUID id) {
        return orders.findById(id).orElseThrow(() -> AppException.notFound("Request", id));
    }

    private static String newToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @ConfigurationProperties("ro.orders")
    record OrdersProperties(String webBaseUrl) {
        OrdersProperties {
            webBaseUrl = webBaseUrl == null ? "http://localhost:4200" : webBaseUrl.replaceAll("/+$", "");
        }
    }
}
