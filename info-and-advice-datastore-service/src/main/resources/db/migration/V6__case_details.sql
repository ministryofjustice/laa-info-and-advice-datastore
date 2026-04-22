CREATE TABLE case_details(
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  reference_number UUID references applications(id),
  require_ecf BOOLEAN NULL,
  has_previous_legal_aid BOOLEAN NULL,
  has_six_months_legal_help BOOLEAN NULL,
  means_assessment_required BOOLEAN NULL,
  type_non_means_tested BOOLEAN NULL, 
  created_at TIMESTAMPTZ NOT NULL DEFAULT(NOW() AT TIME ZONE 'UTC'),
  created_by VARCHAR NOT NULL,
  last_modified_at TIMESTAMPTZ NOT NULL DEFAULT(NOW() AT TIME ZONE 'UTC'),
  last_modified_by VARCHAR NOT NULL
)