-- Rename to match the EligibilityAssessment model (data + CFE result).
-- The created_at trigger, primary key, foreign key and mi_reporting.eligibility_results view
-- follow the table rename, so MI reporting keeps its existing name and columns.
ALTER TABLE eligibility_results RENAME TO eligibility_assessments;
ALTER TABLE eligibility_assessments RENAME COLUMN eligibility_result_id TO eligibility_assessment_id;

-- Temporary: lets pods on the previous release keep reading and writing through the old name
-- during a rolling deploy. A simple view is auto-updatable: inserts use the base table's defaults
-- and triggers, and RETURNING still works. Drop once every environment runs this release.
CREATE VIEW eligibility_results AS
SELECT
    eligibility_assessment_id AS eligibility_result_id,
    application_id,
    data,
    result_json,
    indication,
    contribution,
    created_at,
    created_by
FROM eligibility_assessments;
