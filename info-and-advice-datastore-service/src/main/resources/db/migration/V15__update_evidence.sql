DROP TABLE evidence CASCADE;

ALTER TABLE applications
    DROP COLUMN evidence_id;

ALTER TABLE applications
    ADD COLUMN evidence JSONB;