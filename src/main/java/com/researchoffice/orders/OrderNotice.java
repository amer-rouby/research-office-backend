package com.researchoffice.orders;

/**
 * Published (after commit) when something happens to a request that someone should be told about.
 * Listeners (e.g. the notifications module) decide who receives what.
 */
public record OrderNotice(
        Type type,
        String reference,
        String serviceName,
        String clientName,
        String clientEmail,
        String trackingUrl,
        String adminUrl,
        String detail) {

    public enum Type {
        /** New request from a client. */
        CREATED,
        /** A quote was sent to the client. */
        QUOTED,
        /** The client accepted the quote. */
        ACCEPTED,
        /** An expert submitted work for review. */
        SUBMITTED_FOR_REVIEW,
        /** Work was delivered to the client. */
        DELIVERED,
        /** The client asked for changes. */
        REVISION_REQUESTED,
        /** The request was closed. */
        CLOSED,
        /** A payment was recorded. */
        PAYMENT_RECORDED
    }
}
