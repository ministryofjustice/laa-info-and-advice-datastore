ALTER TABLE events
    ADD COLUMN payload_hash VARCHAR(64);

-- Used to look up whether an identical payload has already been recorded for an application,
-- to detect and reject duplicate create/update/patch requests.
CREATE INDEX IF NOT EXISTS idx_events_duplicate_payload_lookup
    ON events (application_id, provider_office_code, payload_hash);
