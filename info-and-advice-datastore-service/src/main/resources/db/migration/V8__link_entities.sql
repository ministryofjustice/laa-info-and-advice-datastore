ALTER TABLE applications
RENAME COLUMN individual_legal_aid_number to individual_id;

ALTER TABLE individuals
ADD COLUMN address_id UUID NULL CONSTRAINT fk_individuals_address_id REFERENCES addresses(id);

ALTER TABLE applications
ADD COLUMN evidence_id UUID NULL CONSTRAINT fk_applications_evidence_id REFERENCES evidence(id);
ALTER TABLE evidence
DROP COLUMN reference_number;

ALTER TABLE applications
ADD COLUMN case_details_id UUID NOT NULL CONSTRAINT fk_applications_case_details_id REFERENCES case_details(id);
ALTER TABLE case_details
DROP COLUMN reference_number;

ALTER TABLE applications
ADD COLUMN declaration_id UUID NULL CONSTRAINT fk_applications_declaration_id REFERENCES declaration(id);
ALTER TABLE declaration
DROP COLUMN reference_number;