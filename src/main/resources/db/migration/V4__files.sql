CREATE TABLE stored_file (
    id            UUID PRIMARY KEY,
    owner_id      UUID         NOT NULL,
    kind          VARCHAR(20)  NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(150) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    storage_key   VARCHAR(300) NOT NULL UNIQUE,
    uploaded_by   VARCHAR(150) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT stored_file_kind_chk CHECK (kind IN ('CLIENT_UPLOAD', 'DELIVERABLE', 'INTERNAL'))
);
CREATE INDEX stored_file_owner_idx ON stored_file (owner_id);
