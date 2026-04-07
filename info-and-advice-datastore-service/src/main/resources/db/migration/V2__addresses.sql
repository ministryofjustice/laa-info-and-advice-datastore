CREATE TABLE addresses(
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  client_has_home_address BOOLEAN NOT NULL DEFAULT TRUE,
  address_line_1 VARCHAR,
  address_line2 VARCHAR,
  town_or_city VARCHAR,
  post_code VARCHAR,
  created_at TIMESTAMPTZ NOT NULL DEFAULT(NOW() AT TIME ZONE 'UTC'),
  last_modified_at TIMESTAMPTZ NOT NULL DEFAULT(NOW() AT TIME ZONE 'UTC'),
  is_test_data BOOLEAN DEFAULT FALSE
);

INSERT INTO public.addresses(
	client_has_home_address, address_line_1, address_line2, town_or_city, post_code, is_test_data)
	VALUES (TRUE, '123 ABC lane', 'Manny', 'MChester', 'M19 69Y', TRUE);

INSERT INTO public.addresses(
	client_has_home_address, address_line_1, address_line2, town_or_city, post_code, is_test_data)
	VALUES (FALSE, '987 ZXY street', 'Down south', 'Landan', 'SE3 8F', TRUE);