# Information and Advice Data Store API
[![Ministry of Justice Repository Compliance Badge](https://github-community.service.justice.gov.uk/repository-standards/api/laa-spring-boot-microservice-template/badge)](https://github-community.service.justice.gov.uk/repository-standards/laa-spring-boot-microservice-template)

## Overview

Datastore for persisting and retriving data in relation to the information and advice sections of the LAA.

The project uses the `laa-spring-boot-gradle-plugin` Gradle plugin which provides
sensible defaults for the following plugins:

- [Checkstyle](https://docs.gradle.org/current/userguide/checkstyle_plugin.html)
- [Dependency Management](https://plugins.gradle.org/plugin/io.spring.dependency-management)
- [Jacoco](https://docs.gradle.org/current/userguide/jacoco_plugin.html)
- [Java](https://docs.gradle.org/current/userguide/java_plugin.html)
- [Maven Publish](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [Spring Boot](https://plugins.gradle.org/plugin/org.springframework.boot)
- [Test Logger](https://github.com/radarsh/gradle-test-logger-plugin)
- [Versions](https://github.com/ben-manes/gradle-versions-plugin)

The plugin is provided by -  [laa-spring-boot-common](https://github.com/ministryofjustice/laa-spring-boot-common), where you can find
more information regarding setup and usage.

### Project Structure
Includes the following subprojects:

- `info-and-advice-datastore-api` - example OpenAPI specification used for generating API stub interfaces and documentation.
- `info-and-advice-datastore-service` - example REST API service with CRUD operations interfacing a JPA repository with an in-memory database.

## Setup Instructions

### Install pre-hook commits

`scripts/setup-hooks.sh` to install pre-commit hooks this will run
- Spotless on the codebase
- checkStyle on main, test and integration test
- https://github.com/ministryofjustice/devsecops-hooks to scan for any secrets that may accidentally may have been commited. 
- [gitlint](https://github.com/jorisroovers/gitlint) to ensure commit conventions are followed

### Add GitHub Token
Generate a Github PAT (Personal Access Token) to access the required plugin, via https://github.com/settings/tokens

Create a classic PAT token with `repo`, `read:packages` and `write:packages`

Specify the Note field, e.g. “Token to allow access to LAA Gradle plugin”

If you don't already have one, create a `gradle.properties` file in your home directory at `~/.gradle/gradle.properties`.

Add the following properties to `~/.gradle/gradle.properties` and replace the placeholder values as follows:

```
project.ext.gitPackageUser = YOUR_GITHUB_USERNAME
project.ext.gitPackageKey = PAT_CREATED_ABOVE
```

Go back to Github to authorize MOJ for SSO


### Build application
`./gradlew clean build`

### Apply formatting
`./gradlew spotlessApply`

### Check formatting conforms
`./gradlew checkStyleAll`

### Run integration tests
`./gradlew integrationTest`

### Prerequisites

On first setup, add `host.docker.internal` to your `/etc/hosts` (Docker Desktop on Mac does not add this automatically):

```bash
echo '127.0.0.1 host.docker.internal' | sudo tee -a /etc/hosts
```

### Run application locally ignoring auth requirements
`./gradlew bootRunLocal`

### Run full stack via Docker (mock OAuth2 server)

Builds and runs the app, postgres, and a mock OAuth2 server. Requires `.env` to be present:

```bash
cp .env.example .env
./gradlew :info-and-advice-datastore-service:bootJar
docker-compose up -d --build
```

Get an access token and call the API:

```bash
TOKEN=$(curl -s -X POST http://host.docker.internal:9090/default/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=test&client_secret=test" \
  | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v0/applications
```

The mock server automatically issues tokens with the `DataStore.Access` role for `client_credentials` grants.

### Run application with Entra authentication

Copy `.env.example` to `.env` and uncomment the Entra values, filling in the tenant ID and application (client) ID:

```bash
cp .env.example .env
```

| Variable | Description |
|---|---|
| `LAA_OAUTH2_ISSUER_URI` | Entra token issuer URI, e.g. `https://login.microsoftonline.com/<tenant-id>/v2.0` |
| `LAA_OAUTH2_AUDIENCE` | Application (client) ID of this app registration in Entra |

Then export the variables and run:

```bash
set -a && source .env && set +a
./gradlew :info-and-advice-datastore-service:bootRun
```

> **Note:** The app must be run directly (not via Docker) when using Entra, as
> Docker containers on a VPN may not be able to resolve `login.microsoftonline.com`.

## Development guidelines

### Commit conventions
Please follow the (Commit Conventions)[https://www.conventionalcommits.org/en/v1.0.0/] guidelines when making commits.
```
feat: Short desc of feature

Larger body description of feature.
```

### Dependabot
Runs weekly to ensure our library versions are up to date.

### Release-Please 
We use release-please for generating release notes when merging into main, which is why we use the commit conventions to better integrate.

## Deployment information
- our `deploy/` folder plus github actions manage deployments to our resources.
- resources are managed in (cloud-platform-environments)[https://github.com/ministryofjustice/cloud-platform-environments/] under `namespaces/live.cloud-platform.service.justice.gov.uk/laa-info-and-advice-datastore-{ENV}` use the ##ask-cloud-platform slack channel for change request/information.


## Application Endpoints

### API Documentation

#### Swagger UI
- http://localhost:8080/swagger-ui/index.html
#### API docs (JSON)
- http://localhost:8080/v3/api-docs

### Actuator Endpoints
The following actuator endpoints have been configured:
- http://localhost:8080/actuator
- http://localhost:8080/actuator/health

## Additional Information

### Libraries Used
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/index.html) - used to provide various endpoints to help monitor the application, such as view application health and information.
- [Spring Boot Web](https://docs.spring.io/spring-boot/reference/web/index.html) - used to provide features for building the REST API implementation.
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/jpa.html) - used to simplify database access and interaction, by providing an abstraction over persistence technologies, to help reduce boilerplate code.
- [Springdoc OpenAPI](https://springdoc.org/) - used to generate OpenAPI documentation. It automatically generates Swagger UI, JSON documentation based on your Spring REST APIs.
- [Lombok](https://projectlombok.org/) - used to help to reduce boilerplate Java code by automatically generating common
  methods like getters, setters, constructors etc. at compile-time using annotations.
- [MapStruct](https://mapstruct.org/) - used for object mapping, specifically for converting between different Java object types, such as Data Transfer Objects (DTOs)
  and Entity objects. It generates mapping code at compile code.
- [H2](https://www.h2database.com/html/main.html) - used to provide an example database and should not be used in production.


