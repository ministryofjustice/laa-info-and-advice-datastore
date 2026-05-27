ALTER TABLE addresses
DROP COLUMN client_has_home_address;

ALTER TABLE individuals
DROP COLUMN last_name_at_birth;

ALTER TABLE individuals
ADD COLUMN full_name VARCHAR;

UPDATE individuals SET full_name = CONCAT(first_name, ' ', last_name);

ALTER TABLE individuals
ALTER COLUMN full_name SET NOT NULL;

ALTER TABLE individuals
DROP COLUMN first_name,
DROP COLUMN last_name;