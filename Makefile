.PHONY: docker-up docker-up-entra docker-down test-backups smoke-test-backups

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
	PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s deploy/tests -p 'test_rds_backup.py' -v

smoke-test-backups:
	docker run --rm --platform linux/amd64 \
		--user 1000:1000 --read-only --network none \
		--cap-drop ALL --security-opt no-new-privileges \
		--memory 512m --cpus 1 --pids-limit 128 \
		--tmpfs /tmp:rw,exec,nosuid,nodev,size=64m,mode=1777 \
		--mount "type=bind,src=$(CURDIR)/deploy/tests,dst=/workspace/deploy/tests,readonly" \
		--mount "type=bind,src=$(CURDIR)/deploy/laa-info-and-advice-datastore/files,dst=/workspace/deploy/laa-info-and-advice-datastore/files,readonly" \
		--env HOME=/tmp --env TMPDIR=/tmp --env PYTHONDONTWRITEBYTECODE=1 \
		--env AWS_EC2_METADATA_DISABLED=true --env AWS_PAGER= \
		--workdir /workspace --entrypoint python3 \
		ministryofjustice/cloud-platform-tools:2.12.0@sha256:0e6f63db6144437b9daf755653dbec9179a7197c58e38f7c33c87ee22ec165ae \
		/workspace/deploy/tests/smoke_rds_backup.py

full-test: test-backups
	./gradlew checkStyleAll
	./gradlew clean build integrationTest
	docker compose down
	docker compose up -d --build --wait --wait-timeout 180
	cd bruno-collections/info-and-advice-datastore-api-tests && bru run --env=local