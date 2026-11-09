package com.researchoffice.orders.internal;

import com.researchoffice.orders.OrderStatus;
import com.researchoffice.shared.error.AppException;
import com.researchoffice.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A client's request. State changes only through {@link #moveTo} and {@link #quote}. */
@Getter
@Entity
@Table(name = "service_order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ServiceOrder extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(name = "tracking_token", nullable = false, unique = true)
    private String trackingToken;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "client_phone", nullable = false)
    private String clientPhone;

    @Column(name = "client_email")
    private String clientEmail;

    private String country;

    @Column(nullable = false)
    private String details;

    private LocalDate deadline;

    @Column(name = "quoted_price")
    private BigDecimal quotedPrice;

    private String currency;

    @Column(name = "quote_note")
    private String quoteNote;

    ServiceOrder(String reference, String trackingToken, UUID serviceId, String clientName, String clientPhone,
                 String clientEmail, String country, String details, LocalDate deadline) {
        this.reference = reference;
        this.trackingToken = trackingToken;
        this.serviceId = serviceId;
        this.status = OrderStatus.NEW;
        this.clientName = clientName.trim();
        this.clientPhone = clientPhone.trim();
        this.clientEmail = blankToNull(clientEmail);
        this.country = blankToNull(country);
        this.details = details.trim();
        this.deadline = deadline;
    }

    /** Moves to {@code target} if the lifecycle allows it; returns the previous status. */
    OrderStatus moveTo(OrderStatus target) {
        if (!status.canMoveTo(target)) {
            throw AppException.rule("INVALID_TRANSITION", "Cannot move a request from " + status + " to " + target);
        }
        OrderStatus previous = status;
        status = target;
        return previous;
    }

    OrderStatus quote(BigDecimal price, String currency, String note) {
        OrderStatus previous = moveTo(OrderStatus.QUOTED);
        this.quotedPrice = price;
        this.currency = currency.toUpperCase();
        this.quoteNote = blankToNull(note);
        return previous;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
