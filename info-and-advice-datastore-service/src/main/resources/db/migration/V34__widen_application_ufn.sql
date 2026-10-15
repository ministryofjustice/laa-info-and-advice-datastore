DROP VIEW mi_reporting.applications;

ALTER TABLE applications
    ALTER COLUMN ufn TYPE VARCHAR(10);

CREATE VIEW mi_reporting.applications AS
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

GRANT SELECT ON mi_reporting.applications TO mi_reporting_reader;