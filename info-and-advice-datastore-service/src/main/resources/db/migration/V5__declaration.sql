CREATE TABLE declaration(
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  reference_number UUID REFERENCES applications(id),
  client_declaration_status VARCHAR NOT NULL,
  declaration_statement BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT(NOW() AT TIME ZONE 'UTC'),
  created_by VARCHAR NOT NULL,
  last_modified_at TIMESTAMPTZ NOT NULL DEFAULT(NOW() AT TIME ZONE 'UTC'),
  last_modified_by VARCHAR NOT NULL
);