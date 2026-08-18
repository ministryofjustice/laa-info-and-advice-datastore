ALTER TABLE applications RENAME COLUMN provider_firm_id TO provider_firm_code;
ALTER TABLE applications ALTER COLUMN provider_firm_code TYPE VARCHAR USING provider_firm_code::text;

ALTER TABLE events RENAME COLUMN provider_firm_id TO provider_firm_code;
ALTER TABLE events ALTER COLUMN provider_firm_code TYPE VARCHAR USING provider_firm_code::text;
