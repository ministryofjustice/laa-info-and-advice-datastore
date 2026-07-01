ALTER TABLE client_details
    ADD COLUMN first_name VARCHAR(255),
    ADD COLUMN last_name VARCHAR(255);

UPDATE client_details
SET first_name = split_part(full_name, ' ', 1),
    last_name = split_part(full_name, ' ', 2);

ALTER TABLE client_details
    DROP COLUMN full_name;
ALTER TABLE client_details
    ALTER COLUMN first_name SET NOT NULL,
    ALTER COLUMN last_name SET NOT NULL;