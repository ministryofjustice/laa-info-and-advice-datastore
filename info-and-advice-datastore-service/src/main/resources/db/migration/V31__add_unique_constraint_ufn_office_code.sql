-- UFN must be unique among applications sharing the same provider office code.
-- ufn is optional, so applications without a ufn are excluded from the constraint.
CREATE UNIQUE INDEX IF NOT EXISTS uq_applications_office_code_ufn
    ON applications (provider_office_code, ufn)
    WHERE ufn IS NOT NULL;
