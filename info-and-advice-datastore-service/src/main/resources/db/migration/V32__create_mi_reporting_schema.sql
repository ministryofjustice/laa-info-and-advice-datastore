-- Adds a dedicated, PII-free schema for MI/BI tooling (e.g. Metabase).
-- These views expose the same data returned by GET /applications/{id} but with
-- direct client identifiers (name, exact date of birth, NI number, address)
-- removed or reduced so the schema is safe to grant to reporting tools.

CREATE SCHEMA IF NOT EXISTS mi_reporting;

-- Dedicated read-only role for reporting tools. Kept NOLOGIN here; enabling
-- LOGIN and setting a password is an operational task done outside of
-- migrations (e.g. via the managed DB provisioning process/secrets store).
DO
$$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = 'mi_reporting_reader') THEN
    CREATE ROLE mi_reporting_reader NOLOGIN;
  END IF;
END
$$;

REVOKE ALL ON SCHEMA public FROM mi_reporting_reader;
GRANT USAGE ON SCHEMA mi_reporting TO mi_reporting_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA mi_reporting GRANT SELECT ON TABLES TO mi_reporting_reader;

-- Sanitised client details: no first_name, surname, date_of_birth, ni_number,
-- address_id, or link to the addresses table. date_of_birth is reduced to
-- year_of_birth. client_details_id (id) is retained purely as an opaque join
-- key so applications can still be counted/joined per client.
CREATE OR REPLACE VIEW mi_reporting.client_details AS
SELECT
    id,
    EXTRACT(YEAR FROM date_of_birth)::INT AS year_of_birth,
    no_fixed_abode,
    is_test_data,
    created_at,
    created_by,
    modified_at,
    modified_by,
    etag
FROM public.client_details;

-- Applications contain no direct client identifiers; passed through as-is.
CREATE OR REPLACE VIEW mi_reporting.applications AS
SELECT
    id,
    client_details_id,
    provider_firm_code,
    provider_office_code,
    application_state,
    reason_for_reapplication,
    means_assessment_required,
    type_of_non_means,
    ecf_flag,
    case_id,
    contribution,
    application_type,
    determination_id,
    laa_reference,
    ufn,
    is_means_tested,
    scoping_questions,
    data_retention_event_uuid,
    data_retention_date,
    created_at,
    created_by,
    modified_at,
    modified_by,
    etag
FROM public.applications;

-- Declarations contain no client identifiers; passed through as-is.
CREATE OR REPLACE VIEW mi_reporting.declaration AS
SELECT
    id,
    client_declaration_status,
    declaration_confirmation,
    date_signed,
    created_at,
    created_by,
    modified_at,
    modified_by,
    etag
FROM public.declaration;

-- Evidence: the checklist columns are excluded, as they hold free-form JSON
-- keyed by evidence document type and are treated as sensitive.
CREATE OR REPLACE VIEW mi_reporting.evidence AS
SELECT
    evidence_id,
    evidence_exemption_code,
    evidence_exemption_reason,
    created_at,
    created_by,
    modified_at,
    modified_by
FROM public.evidence;

-- Eligibility results: "data" (raw means-assessment answers, see
-- EligibilityData in the API spec) and "result_json" (raw CFE calculation
-- payload) are excluded, as they hold detailed client financial/circumstance
-- data (income, benefits, bank accounts, dependants, etc.) that is treated as
-- sensitive. Only the summarised outcome fields are exposed.
CREATE OR REPLACE VIEW mi_reporting.eligibility_results AS
SELECT
    eligibility_result_id,
    application_id,
    indication,
    contribution,
    created_at,
    created_by
FROM public.eligibility_results;

-- Note: the "events" table (raw HTTP request/response payloads) and the
-- "addresses" table (full client address) are intentionally NOT exposed here,
-- as they can contain client PII that isn't safe for general reporting access.

GRANT SELECT ON
    mi_reporting.client_details,
    mi_reporting.applications,
    mi_reporting.declaration,
    mi_reporting.evidence,
    mi_reporting.eligibility_results
TO mi_reporting_reader;
