-- Move date_declaration_was_signed from applications to declaration table
ALTER TABLE declaration ADD COLUMN IF NOT EXISTS date_signed DATE;
ALTER TABLE applications DROP COLUMN IF EXISTS date_declaration_was_signed;

-- Remove default from declaration_confirmation - value must be explicitly provided
ALTER TABLE declaration ALTER COLUMN declaration_confirmation DROP DEFAULT;
