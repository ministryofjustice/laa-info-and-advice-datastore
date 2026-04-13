CREATE TABLE applications(
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  individual_legal_aid_number UUID NOT NULL references individuals(id),
  provider_firm_id UUID NOT NULL,
  provider_office_id UUID NOT NULL,
  eligibility_result_id UUID NULL,
  client_case_details_status VARCHAR NOT NULL,
  means_assessment_status_id UUID NULL,
  evidence_status_id UUID NULL,
  client_declaration_status_id UUID NULL,
  overall_application_status VARCHAR NOT NULL,
  unique_file_number UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT(NOW() AT TIME ZONE 'UTC'),
  created_by VARCHAR NOT NULL,
  last_modified_at TIMESTAMPTZ NOT NULL DEFAULT(NOW() AT TIME ZONE 'UTC'),
  last_modified_by VARCHAR NOT NULL
);