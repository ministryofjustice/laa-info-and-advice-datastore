--G, I, O, S, Z removed for being disallowed characters
CREATE OR REPLACE FUNCTION generate_random_string (INT) RETURNS TEXT AS $$
DECLARE
  allowed_characters TEXT := '0123456789ABCDEFHJKLMNPQRTUVWXY';
  allowed_characters_count INTEGER := LENGTH(allowed_characters);
 BEGIN
    RETURN(SELECT string_agg(substring(allowed_characters, random(1, allowed_characters_count), 1), '') FROM generate_series(1, $1));
  END
$$ LANGUAGE plpgsql;
