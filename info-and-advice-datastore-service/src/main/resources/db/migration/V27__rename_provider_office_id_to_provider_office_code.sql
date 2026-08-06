ALTER TABLE applications RENAME COLUMN provider_office_id TO provider_office_code;
ALTER TABLE applications ALTER COLUMN provider_office_code TYPE VARCHAR USING provider_office_code::text;

ALTER TABLE events RENAME COLUMN provider_office_id TO provider_office_code;
ALTER TABLE events ALTER COLUMN provider_office_code TYPE VARCHAR USING provider_office_code::text;
