ALTER TABLE applications
DROP COLUMN IF EXISTS means_assessment_id;

ALTER TABLE eligibility_results
RENAME COLUMN created_date TO created_at;