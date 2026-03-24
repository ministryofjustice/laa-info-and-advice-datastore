CREATE TABLE individuals(
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  first_name VARCHAR NOT NULL,
  last_name VARCHAR NOT NULL,
  date_of_birth DATE NOT NULL,
  last_name_at_birth VARCHAR,
  ni_number VARCHAR(9),
  created_at TIMESTAMPTZ NOT NULL DEFAULT(NOW() AT TIME ZONE 'UTC'),
  last_modified_at TIMESTAMPTZ NOT NULL DEFAULT(NOW() AT TIME ZONE 'UTC'),
  is_test_data BOOLEAN DEFAULT FALSE
);

---Add some test data to seed the database, use is_test_data
INSERT INTO public.individuals(
	first_name, last_name, date_of_birth, last_name_at_birth, ni_number, is_test_data)
	VALUES ('Jimi', 'Hendrix', '1942-09-27', NULL, 'PH324585Q', TRUE);

INSERT INTO public.individuals(
	first_name, last_name, date_of_birth, last_name_at_birth, ni_number, is_test_data)
	VALUES ('Robert', 'Johnson', '1911-05-08', NULL, NULL, TRUE);

INSERT INTO public.individuals(
	first_name, last_name, date_of_birth, last_name_at_birth, ni_number, is_test_data)
	VALUES ('Janis', 'Joplin', '1943-01-19', NULL, 'HG077876G', TRUE);

INSERT INTO public.individuals(
	first_name, last_name, date_of_birth, last_name_at_birth, ni_number, is_test_data)
	VALUES ('Kurt', 'Cobain', '1967-02-20', 'kobain', NULL, TRUE);

INSERT INTO public.individuals(
	first_name, last_name, date_of_birth, last_name_at_birth, ni_number, is_test_data)
	VALUES ('Amy', 'Winehouse', '1983-09-14', 'winehause', 'EX348350D', TRUE);

