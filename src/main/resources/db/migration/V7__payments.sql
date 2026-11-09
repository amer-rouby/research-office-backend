CREATE TABLE order_payment (
    id          UUID PRIMARY KEY,
    order_id    UUID           NOT NULL REFERENCES service_order (id) ON DELETE CASCADE,
    amount      NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency    VARCHAR(3)     NOT NULL,
    method      VARCHAR(20)    NOT NULL,
    paid_on     DATE           NOT NULL,
    note        VARCHAR(500),
    recorded_by VARCHAR(150)   NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL,
    version     BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT order_payment_method_chk CHECK (method IN ('BANK_TRANSFER', 'CASH', 'PAYPAL', 'WISE', 'WALLET', 'OTHER'))
);
CREATE INDEX order_payment_order_idx ON order_payment (order_id, paid_on);
