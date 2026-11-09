package com.researchoffice.orders;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle of a request. The transition table below is the single source of truth:
 * the API rejects any move it does not list, and the web app reads {@code next()} to offer actions.
 */
public enum OrderStatus {
    NEW, QUOTED, ACCEPTED, IN_PROGRESS, IN_REVIEW, DELIVERED, REVISION, CLOSED, CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> NEXT = new EnumMap<>(OrderStatus.class);

    static {
        NEXT.put(NEW, EnumSet.of(QUOTED, CANCELLED));
        NEXT.put(QUOTED, EnumSet.of(QUOTED, ACCEPTED, CANCELLED));
        NEXT.put(ACCEPTED, EnumSet.of(IN_PROGRESS, CANCELLED));
        NEXT.put(IN_PROGRESS, EnumSet.of(IN_REVIEW, CANCELLED));
        NEXT.put(IN_REVIEW, EnumSet.of(DELIVERED, IN_PROGRESS));
        NEXT.put(DELIVERED, EnumSet.of(CLOSED, REVISION));
        NEXT.put(REVISION, EnumSet.of(IN_PROGRESS, IN_REVIEW));
        NEXT.put(CLOSED, EnumSet.noneOf(OrderStatus.class));
        NEXT.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    public Set<OrderStatus> next() {
        return EnumSet.copyOf((EnumSet<OrderStatus>) NEXT.get(this));
    }

    public boolean canMoveTo(OrderStatus target) {
        return NEXT.get(this).contains(target);
    }

    public boolean isOpen() {
        return this != CLOSED && this != CANCELLED;
    }
}
