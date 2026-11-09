package com.researchoffice.orders.internal;

import com.researchoffice.orders.OrderStatus;
import com.researchoffice.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One line of a request's timeline. */
@Getter
@Entity
@Table(name = "order_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class OrderEvent extends BaseEntity {

    enum Type { CREATED, STATUS_CHANGED, QUOTED, NOTE, FILE_ADDED, PAYMENT }

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private OrderStatus toStatus;

    private String note;

    @Column(nullable = false)
    private String actor;

    @Column(name = "visible_to_client", nullable = false)
    private boolean visibleToClient;

    OrderEvent(UUID orderId, Type type, OrderStatus from, OrderStatus to, String note, String actor, boolean visible) {
        this.orderId = orderId;
        this.type = type;
        this.fromStatus = from;
        this.toStatus = to;
        this.note = note == null || note.isBlank() ? null : note.trim();
        this.actor = actor;
        this.visibleToClient = visible;
    }
}
