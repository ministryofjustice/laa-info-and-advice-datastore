.PHONY: docker-up docker-up-entra docker-down test-backups smoke-test-backups test-backup-chart

# Fail each recipe line on any error, including within pipelines.
SHELL := /bin/bash
.SHELLFLAGS := -eu -o pipefail -c

BACKUP_TEST_IMAGE := public.ecr.aws/lambda/python:3.13@sha256:eac388575a9ff5c116d32b9620675341f998def5fe224fc3752186986afa9a4a
BACKUP_TEST_CONTAINER = docker run --rm --platform linux/amd64 \
	--user 1000:1000 --read-only --network none \
	--cap-drop ALL --security-opt no-new-privileges \
	--memory 512m --cpus 1 --pids-limit 128 \
	--tmpfs /tmp:rw,noexec,nosuid,nodev,size=64m,mode=1777 \
	--mount "type=bind,src=$(CURDIR)/deploy/tests,dst=/workspace/deploy/tests,readonly" \
	--mount "type=bind,src=$(CURDIR)/deploy/laa-info-and-advice-datastore/files,dst=/workspace/deploy/laa-info-and-advice-datastore/files,readonly" \
	--env HOME=/tmp --env TMPDIR=/tmp --env PYTHONDONTWRITEBYTECODE=1 \
	--env AWS_EC2_METADATA_DISABLED=true \
	--workdir /workspace --entrypoint python3 $(BACKUP_TEST_IMAGE)

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
	$(BACKUP_TEST_CONTAINER) -m unittest discover -s deploy/tests -p 'test_rds_backup.py' -v

smoke-test-backups:
	$(BACKUP_TEST_CONTAINER) /workspace/deploy/tests/smoke_rds_backup.py

test-backup-chart:
	PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s deploy/tests -p 'test_backup_chart.py' -v
	@for environment in uat staging production; do \
		namespace="laa-info-and-advice-datastore-$${environment/production/prod}"; \
		helm lint ./deploy/laa-info-and-advice-datastore --quiet \
			--namespace "$$namespace" --kube-version 1.30.0 \
			--values "./deploy/laa-info-and-advice-datastore/values/$$environment.yaml" \
			--set backups.enabled=false; \
	done
	bash -n deploy/scripts/deploy.sh

full-test: test-backups test-backup-chart
	./gradlew checkStyleAll
	./gradlew clean build integrationTest
	docker compose down
	docker compose up -d --build --wait --wait-timeout 180
	cd bruno-collections/info-and-advice-datastore-api-tests && bru run --env=local