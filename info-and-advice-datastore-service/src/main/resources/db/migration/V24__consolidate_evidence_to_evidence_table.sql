ALTER TABLE applications DROP COLUMN IF EXISTS evidence;

ALTER TABLE evidence DROP COLUMN IF EXISTS case_id;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS evidence_id UUID REFERENCES evidence(evidence_id);
