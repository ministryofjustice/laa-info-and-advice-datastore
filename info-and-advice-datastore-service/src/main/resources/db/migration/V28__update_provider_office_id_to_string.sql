ALTER TABLE applications ALTER COLUMN provider_office_id TYPE VARCHAR USING provider_office_id::text;

ALTER TABLE events ALTER COLUMN provider_office_id TYPE VARCHAR USING provider_office_id::text;
