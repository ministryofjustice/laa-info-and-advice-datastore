-- Widens two of the mi_reporting views added in V32, following review with
-- product/MI-BI:

-- Evidence: the checklist columns just record which evidence document types
-- the provider has collected from the client (not the evidence itself), so
-- they carry no PII and are safe to expose as-is.
CREATE OR REPLACE VIEW mi_reporting.evidence AS
SELECT
    evidence_id,
    evidence_exemption_code,
    evidence_exemption_reason,
    created_at,
    created_by,
    modified_at,
    modified_by,
    income_evidence_checklist,
    expenditure_capital_evidence_checklist
FROM public.evidence;

-- Eligibility results: pre-launch, MI/BI have not yet scoped exact reporting
-- requirements, so everything except the "pending" and "api_response" keys
-- within "data" (interim/raw journey state, not final answers - see
-- EligibilityData in the API spec) is exposed for now. None of these fields
-- are direct client identifiers (no name, DOB, NI number, address); revisit
-- and tighten (e.g. an allowlist) once real requirements/live-traffic risk
-- are established.
CREATE OR REPLACE VIEW mi_reporting.eligibility_results AS
SELECT
    eligibility_result_id,
    application_id,
    indication,
    contribution,
    created_at,
    created_by,
    (data - 'pending' - 'api_response') AS data,
    result_json
FROM public.eligibility_results;
