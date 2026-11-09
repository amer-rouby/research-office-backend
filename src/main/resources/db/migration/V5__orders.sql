CREATE SEQUENCE order_ref_seq START 1;

CREATE TABLE service_order (
    id             UUID PRIMARY KEY,
    reference      VARCHAR(20)   NOT NULL UNIQUE,
    tracking_token VARCHAR(64)   NOT NULL UNIQUE,
    service_id     UUID          NOT NULL REFERENCES service_offering (id),
    status         VARCHAR(20)   NOT NULL,
    client_name    VARCHAR(150)  NOT NULL,
    client_phone   VARCHAR(30)   NOT NULL,
    client_email   VARCHAR(190),
    country        VARCHAR(60),
    details        VARCHAR(4000) NOT NULL,
    deadline       DATE,
    quoted_price   NUMERIC(12, 2),
    currency       VARCHAR(3),
    quote_note     VARCHAR(1000),
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,
    version        BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX service_order_status_idx ON service_order (status);
CREATE INDEX service_order_created_idx ON service_order (created_at DESC);

CREATE TABLE order_event (
    id                UUID PRIMARY KEY,
    order_id          UUID          NOT NULL REFERENCES service_order (id) ON DELETE CASCADE,
    type              VARCHAR(20)   NOT NULL,
    from_status       VARCHAR(20),
    to_status         VARCHAR(20),
    note              VARCHAR(2000),
    actor             VARCHAR(150)  NOT NULL,
    visible_to_client BOOLEAN       NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    version           BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX order_event_order_idx ON order_event (order_id, created_at);
