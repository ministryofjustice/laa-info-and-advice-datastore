.PHONY: docker-up docker-up-entra docker-down

docker-up:
	docker compose up -d --build

# Sign in via real Entra ID instead of the default mock-oauth2-server - see .env.entra.
docker-up-entra:
	op run --env-file=.env \
		--env-file=.env.entra \
		-- docker compose up -d --build
