CREATE TABLE eligibility_results (
  eligibility_result_id UUID PRIMARY KEY DEFAULT uuidv7(),
  application_id UUID NOT NULL REFERENCES applications(id),
  result_json JSONB NOT NULL,
  created_date TIMESTAMPTZ NOT NULL DEFAULT(NOW() AT TIME ZONE 'UTC')
);
