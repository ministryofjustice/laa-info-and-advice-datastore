.PHONY: docker-up docker-up-entra docker-down

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

full-test:
	./gradlew checkStyleAll
	./gradlew clean build integrationTest
	docker compose down
	docker compose up -d --build --wait --wait-timeout 180
	cd bruno-collections/info-and-advice-datastore-api-tests && bru run --env=local