CREATE TABLE entity_changes
(
    id             BIGSERIAL    PRIMARY KEY,
    entity_type    VARCHAR(255) NOT NULL,
    entity_id      UUID         NOT NULL,
    changed_fields JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX entity_changes_entity_idx ON entity_changes (entity_type, entity_id);
