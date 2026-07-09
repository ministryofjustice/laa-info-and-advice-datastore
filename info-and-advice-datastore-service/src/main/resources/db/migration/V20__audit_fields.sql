ALTER TABLE addresses
ADD COLUMN IF NOT EXISTS created_by VARCHAR NULL,
ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR NULL;
ALTER TABLE client_details
ADD COLUMN IF NOT EXISTS created_by VARCHAR NULL,
ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR NULL;
ALTER TABLE eligibility_results
ADD COLUMN IF NOT EXISTS created_by VARCHAR NULL;

UPDATE addresses SET created_by = 'system', last_modified_by = 'system' WHERE created_by IS NULL;
UPDATE client_details SET created_by = 'system', last_modified_by = 'system' WHERE created_by IS NULL;
UPDATE eligibility_results SET created_by = 'system' WHERE created_by IS NULL;

ALTER TABLE addresses
ALTER COLUMN created_by SET NOT NULL;

ALTER TABLE client_details
ALTER COLUMN created_by SET NOT NULL;

ALTER TABLE eligibility_results
ALTER COLUMN created_by SET NOT NULL;

CREATE OR REPLACE FUNCTION modified_at_trigger() RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
BEGIN
  NEW.last_modified_at = NOW() AT TIME ZONE 'UTC';
  NEW.created_at = OLD.created_at;
  NEW.created_by = OLD.created_by;
  RETURN NEW;
END
$$;

CREATE OR REPLACE FUNCTION created_at_modified_at_trigger() RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
BEGIN
  NEW.created_at = NOW() AT TIME ZONE 'UTC';
  NEW.last_modified_at = NOW() AT TIME ZONE 'UTC';
  RETURN NEW;
END
$$;

CREATE OR REPLACE FUNCTION created_at_trigger() RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
BEGIN
  NEW.created_at = NOW() AT TIME ZONE 'UTC';
  RETURN NEW;
END
$$;

CREATE OR REPLACE TRIGGER address_created_at_trigger BEFORE INSERT ON addresses FOR EACH ROW EXECUTE PROCEDURE created_at_modified_at_trigger();
CREATE OR REPLACE TRIGGER address_modified_at_trigger BEFORE UPDATE ON addresses FOR EACH ROW EXECUTE PROCEDURE modified_at_trigger();

CREATE OR REPLACE TRIGGER application_created_at_trigger BEFORE INSERT ON applications FOR EACH ROW EXECUTE PROCEDURE created_at_modified_at_trigger();
CREATE OR REPLACE TRIGGER application_modified_at_trigger BEFORE UPDATE ON applications FOR EACH ROW EXECUTE PROCEDURE modified_at_trigger();

CREATE OR REPLACE TRIGGER client_details_created_at_trigger BEFORE INSERT ON client_details FOR EACH ROW EXECUTE PROCEDURE created_at_modified_at_trigger();
CREATE OR REPLACE TRIGGER client_details_modified_at_trigger BEFORE UPDATE ON client_details FOR EACH ROW EXECUTE PROCEDURE modified_at_trigger();

CREATE OR REPLACE TRIGGER declarations_created_at_trigger BEFORE INSERT ON declaration FOR EACH ROW EXECUTE PROCEDURE created_at_modified_at_trigger();
CREATE OR REPLACE TRIGGER declarations_modified_at_trigger BEFORE UPDATE ON declaration FOR EACH ROW EXECUTE PROCEDURE modified_at_trigger();

CREATE OR REPLACE TRIGGER eligibility_results_created_at_trigger BEFORE INSERT ON eligibility_results FOR EACH ROW EXECUTE PROCEDURE created_at_trigger();
CREATE OR REPLACE TRIGGER events_created_at_trigger BEFORE INSERT ON events FOR EACH ROW EXECUTE PROCEDURE created_at_trigger();