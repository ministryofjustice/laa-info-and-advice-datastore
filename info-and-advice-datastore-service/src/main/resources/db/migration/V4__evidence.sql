CREATE TABLE evidence(
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  reference_number UUID REFERENCES applications(id),
  evidence_status VARCHAR,
  paye_income_evidence BOOLEAN DEFAULT FALSE,
  other_income_evidence BOOLEAN DEFAULT FALSE,
  housing_costs_evidence BOOLEAN DEFAULT FALSE,
  capital_evidence BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT(NOW() AT TIME ZONE 'UTC'),
  created_by VARCHAR NOT NULL,
  last_modified_at TIMESTAMPTZ NOT NULL DEFAULT(NOW() AT TIME ZONE 'UTC'),
  last_modified_by VARCHAR NOT NULL
);