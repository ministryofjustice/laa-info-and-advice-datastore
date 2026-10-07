ALTER TABLE applications ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE applications ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE client_details ALTER COLUMN first_name DROP NOT NULL;
ALTER TABLE client_details ALTER COLUMN surname DROP NOT NULL;
ALTER TABLE client_details ALTER COLUMN date_of_birth DROP NOT NULL;

ALTER TABLE addresses ALTER COLUMN address_line_1 DROP NOT NULL;
