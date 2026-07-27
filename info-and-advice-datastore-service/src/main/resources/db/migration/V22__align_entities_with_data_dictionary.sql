-- Rename columns in applications to match data dictionary
ALTER TABLE applications RENAME COLUMN reference_number TO case_id;
ALTER TABLE applications RENAME COLUMN last_modified_at TO modified_at;
ALTER TABLE applications RENAME COLUMN last_modified_by TO modified_by;

-- Add missing columns to applications
ALTER TABLE applications ADD COLUMN IF NOT EXISTS laa_reference VARCHAR;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS scoping_routing_code VARCHAR;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS ufn VARCHAR(9);
ALTER TABLE applications ADD COLUMN IF NOT EXISTS date_declaration_was_signed DATE;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS data_retention_event_uuid UUID;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS data_retention_date TIMESTAMP WITH TIME ZONE;

-- Rename columns in client_details to match data dictionary
ALTER TABLE client_details RENAME COLUMN last_name TO surname;
ALTER TABLE client_details RENAME COLUMN last_modified_at TO modified_at;
ALTER TABLE client_details RENAME COLUMN last_modified_by TO modified_by;

-- Add missing columns to client_details
ALTER TABLE client_details ADD COLUMN IF NOT EXISTS no_fixed_abode BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE client_details ADD COLUMN IF NOT EXISTS data_retention_event_uuid UUID;
ALTER TABLE client_details ADD COLUMN IF NOT EXISTS data_retention_date TIMESTAMP WITH TIME ZONE;

-- Rename columns in addresses to match data dictionary
ALTER TABLE addresses RENAME COLUMN town_or_city TO town_city;
ALTER TABLE addresses RENAME COLUMN post_code TO postcode;
ALTER TABLE addresses RENAME COLUMN last_modified_at TO modified_at;
ALTER TABLE addresses RENAME COLUMN last_modified_by TO modified_by;

-- Rename columns in declaration for trigger consistency
ALTER TABLE declaration RENAME COLUMN last_modified_at TO modified_at;
ALTER TABLE declaration RENAME COLUMN last_modified_by TO modified_by;

-- Add missing columns to eligibility_results
ALTER TABLE eligibility_results ADD COLUMN IF NOT EXISTS indication BOOLEAN;
ALTER TABLE eligibility_results ADD COLUMN IF NOT EXISTS contribution VARCHAR;

-- Create evidence table per data dictionary
CREATE TABLE IF NOT EXISTS evidence (
    evidence_id UUID NOT NULL DEFAULT gen_random_uuid(),
    case_id VARCHAR NOT NULL,
    evidence_exemption_code VARCHAR,
    evidence_exemption_reason VARCHAR(400),
    income_evidence_checklist JSONB,
    expenditure_capital_evidence_checklist JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR NOT NULL,
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    modified_by VARCHAR NOT NULL,
    PRIMARY KEY (evidence_id)
);

-- Update trigger function to use renamed modified_at column (was last_modified_at)
CREATE OR REPLACE FUNCTION modified_at_trigger() RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
BEGIN
  NEW.modified_at = NOW() AT TIME ZONE 'UTC';
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
  NEW.modified_at = NOW() AT TIME ZONE 'UTC';
  RETURN NEW;
END
$$;

-- Add triggers for evidence table
CREATE OR REPLACE TRIGGER evidence_created_at_trigger BEFORE INSERT ON evidence FOR EACH ROW EXECUTE PROCEDURE created_at_modified_at_trigger();
CREATE OR REPLACE TRIGGER evidence_modified_at_trigger BEFORE UPDATE ON evidence FOR EACH ROW EXECUTE PROCEDURE modified_at_trigger();

-- Update reference_number_trigger to use renamed case_id column
CREATE OR REPLACE FUNCTION reference_number_trigger() RETURNS TRIGGER
LANGUAGE plpgsql
AS
$$
  DECLARE reference_number_to_insert TEXT;
  DECLARE generate_new_reference BOOLEAN := TRUE;
  BEGIN
    WHILE generate_new_reference LOOP
      reference_number_to_insert = generate_reference_number();
      generate_new_reference = EXISTS(SELECT 1 FROM applications WHERE case_id = reference_number_to_insert);
      new.case_id := reference_number_to_insert;
    END LOOP;
    return new;
  END
$$;
