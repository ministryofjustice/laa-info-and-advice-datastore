.PHONY: docker-up docker-up-entra docker-down test-backups

# Fail each recipe line on any error, including within pipelines.
SHELL := /bin/bash
.SHELLFLAGS := -eu -o pipefail -c

docker-up:
	docker compose up -d --build

# Sign in via real Entra ID instead of the default mock-oauth2-server - see .env.entra.
docker-up-entra:
	op run --env-file=.env \
		--env-file=.env.entra \
		-- docker compose up -d --build

bruno:
	cd bruno-collections/info-and-advice-datastore-api-tests && bru run --env=local

test-backups:
	sh -n deploy/laa-info-and-advice-datastore/files/rds-backup.sh
	PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s deploy/tests -p 'test_rds_backup.py' -v

full-test: test-backups
	./gradlew checkStyleAll
	./gradlew clean build integrationTest
	docker compose down
	docker compose up -d --build --wait --wait-timeout 180
	cd bruno-collections/info-and-advice-datastore-api-tests && bru run --env=local