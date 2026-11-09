package com.researchoffice.orders.internal;

import com.researchoffice.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;

/** Money received for a request (recorded by staff; online payment gateways come later). */
@Getter
@Entity
@Table(name = "order_payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class OrderPayment extends BaseEntity {

    enum Method { BANK_TRANSFER, CASH, PAYPAL, WISE, WALLET, OTHER }

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Method method;

    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn;

    private String note;

    @Column(name = "recorded_by", nullable = false)
    private String recordedBy;

    OrderPayment(UUID orderId, BigDecimal amount, String currency, Method method, LocalDate paidOn, String note,
                 String recordedBy) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.method = method;
        this.paidOn = paidOn;
        this.note = note == null || note.isBlank() ? null : note.trim();
        this.recordedBy = recordedBy;
    }
}

interface OrderPaymentRepository extends JpaRepository<OrderPayment, UUID> {

    List<OrderPayment> findByOrderIdOrderByPaidOnAscCreatedAtAsc(UUID orderId);

    @org.springframework.data.jpa.repository.Query(
            "select p.currency as currency, sum(p.amount) as total from OrderPayment p where p.paidOn >= :from group by p.currency")
    List<CurrencySum> paidSince(LocalDate from);

    @org.springframework.data.jpa.repository.Query("select p.currency as currency, sum(p.amount) as total "
            + "from OrderPayment p, ServiceOrder o where o.id = p.orderId and o.status in :statuses group by p.currency")
    List<CurrencySum> paidForStatuses(java.util.Collection<com.researchoffice.orders.OrderStatus> statuses);
}
