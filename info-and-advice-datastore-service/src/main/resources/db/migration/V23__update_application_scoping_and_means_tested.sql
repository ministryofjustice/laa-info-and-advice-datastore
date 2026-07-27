-- Replace scoping_routing_code with scoping_questions (JSONB) per updated data dictionary
ALTER TABLE applications DROP COLUMN IF EXISTS scoping_routing_code;
ALTER TABLE applications ADD COLUMN IF NOT EXISTS scoping_questions JSONB;

-- Add is_means_tested column per updated data dictionary
ALTER TABLE applications ADD COLUMN IF NOT EXISTS is_means_tested BOOLEAN;
