package com.researchoffice.orders.internal;

import com.researchoffice.orders.OrderStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

interface OrderRepository extends JpaRepository<ServiceOrder, UUID>, JpaSpecificationExecutor<ServiceOrder> {

    Optional<ServiceOrder> findByTrackingToken(String trackingToken);

    List<ServiceOrder> findByAssignedExpertIdOrderByUpdatedAtDesc(UUID expertId);

    @Query(value = "SELECT nextval('order_ref_seq')", nativeQuery = true)
    long nextReferenceNumber();

    @Query("select o.status as status, count(o) as total from ServiceOrder o group by o.status")
    List<StatusCount> countByStatus();

    interface StatusCount {
        OrderStatus getStatus();

        long getTotal();
    }

    static Specification<ServiceOrder> matching(OrderStatus status, String query) {
        Specification<ServiceOrder> spec = Specification.unrestricted();
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("clientName")), like),
                    cb.like(cb.lower(root.get("reference")), like),
                    cb.like(root.get("clientPhone"), like)));
        }
        return spec;
    }
}

interface OrderEventRepository extends JpaRepository<OrderEvent, UUID> {

    List<OrderEvent> findByOrderIdOrderByCreatedAt(UUID orderId);
}
