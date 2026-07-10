CREATE TABLE events
(
    sequence_number   BIGSERIAL     PRIMARY KEY,
    changed_by        VARCHAR(255)  NOT NULL,
    provider_firm_id  UUID,
    provider_office_id UUID,
    http_method       VARCHAR(10)   NOT NULL,
    url_path          VARCHAR(2048) NOT NULL,
    payload           JSONB,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);
