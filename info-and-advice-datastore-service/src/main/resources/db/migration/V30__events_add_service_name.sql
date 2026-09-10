ALTER TABLE events
    ADD COLUMN IF NOT EXISTS service_name VARCHAR(255);

UPDATE events SET service_name = 'unknown-service' WHERE service_name IS NULL;

ALTER TABLE events
    ALTER COLUMN service_name SET NOT NULL;
