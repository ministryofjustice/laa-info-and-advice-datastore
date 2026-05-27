--G, I, O, S, Z removed for being disallowed characters
CREATE OR REPLACE FUNCTION generate_random_string (INT) RETURNS TEXT AS $$
DECLARE
  allowed_characters TEXT := '0123456789ABCDEFHJKLMNPQRTUVWXY';
  allowed_characters_count INTEGER := LENGTH(allowed_characters);
 BEGIN
    RETURN(SELECT string_agg(substring(allowed_characters, round(random() * allowed_characters_count)::integer, 1), '') FROM generate_series(1, $1));
  END
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION generate_reference_number() RETURNS TEXT
LANGUAGE sql
AS
$$
    --example generation: L-KTD-RJB
	SELECT CONCAT('L-', generate_random_string (3), '-', generate_random_string (3))
$$;

ALTER TABLE applications
DROP COLUMN case_details_id;

DROP TABLE case_details;

ALTER TABLE applications
ADD COLUMN reason_for_reapplication VARCHAR NULL,
ADD COLUMN means_assessment_required BOOLEAN NULL,
ADD COLUMN type_of_non_means BOOLEAN NULL,
ADD COLUMN ecf_flag BOOLEAN NULL,
ADD COLUMN reference_number VARCHAR NULL,
ADD COLUMN contribution VARCHAR NULL,
ADD COLUMN application_type VARCHAR,
ADD COLUMN determination_id UUID NULL;

ALTER TABLE applications
RENAME COLUMN overall_application_status to application_state;
ALTER TABLE applications
RENAME COLUMN individual_id to client_details_id;
ALTER TABLE applications
RENAME COLUMN means_assessment_status_id to means_assessment_id;

UPDATE applications SET reference_number = generate_reference_number();
UPDATE applications SET application_type = 'RCW' WHERE application_type IS NULL;

ALTER TABLE applications
ALTER COLUMN reference_number SET NOT NULL,
ALTER COLUMN application_type SET NOT NULL;

CREATE OR REPLACE FUNCTION reference_number_trigger() RETURNS TRIGGER
LANGUAGE plpgsql 
AS 
$$
  DECLARE reference_number_to_insert TEXT;
  DECLARE generate_new_reference BOOLEAN := TRUE;
  BEGIN
    WHILE generate_new_reference LOOP
      reference_number_to_insert = generate_reference_number();
	    generate_new_reference = EXISTS(SELECT 1 FROM applications WHERE reference_number = reference_number_to_insert);
      new.reference_number := reference_number_to_insert;
	END LOOP;
	return new;
  END
$$;

CREATE OR REPLACE TRIGGER reference_number_trigger BEFORE INSERT ON applications FOR EACH ROW EXECUTE PROCEDURE reference_number_trigger();

ALTER TABLE applications
DROP COLUMN client_case_details_status,
DROP COLUMN evidence_status_id,
DROP COLUMN client_declaration_status_id,
DROP COLUMN unique_file_number,
DROP COLUMN eligibility_result_id;

ALTER TABLE individuals
  RENAME TO client_details;