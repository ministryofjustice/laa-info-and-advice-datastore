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

### Run full stack via Docker (mock OAuth2 server)

Builds and runs the app, postgres, and a mock OAuth2 server. Requires `.env` to be present:

```bash
cp .env.example .env
make docker-up
```

Get an access token and call the API:

```bash
TOKEN=$(curl -s -X POST http://host.docker.internal:9090/default/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=test&client_secret=test" \
  | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" -H "X-Authorization: Bearer $TOKEN" http://localhost:8080/api/v0/applications
```

The mock server automatically issues tokens with the `DataStore.Access` scope for `client_credentials` grants and includes a `FIRM_CODE` claim so the same token can be used for both 
auth headers when developing.

Stop the stack with `make docker-down`.

#### Switching between the mock IdP and real Entra ID

By default, `make docker-up` validates tokens against the `mock-oauth2-server` container. 

To instead validate real Entra ID tokens (e.g. when running as part of the full stack from [laa-record-controlled-work](https://github.com/ministryofjustice/laa-record-controlled-work)),
run `make docker-up-entra` - this relies on having a `.env.entra` file, which you can copy from `.env.entra.example`.

Variable substitution falls back to the mock server defaults when unset. `.env.entra` is the single source of truth for this API's own Entra config, whether it's run standalone or as part of the full stack.

### Run application with Entra authentication

Copy `.env.entra.example` to `.env.entra` (kept out of git, unlike `.env.entra.example` - it only
ever holds 1Password references, never real secret values, so there's nothing developer-specific
to set up). Export the variables and run:

| Variable | Description |
|---|---|
| `LAA_OAUTH2_ISSUER_URI` | Entra token issuer URI, e.g. `https://login.microsoftonline.com/<tenant-id>/v2.0` |
| `LAA_OAUTH2_AUDIENCE` | Application (client) ID of this app registration in Entra |
| `TRUSTED_CALLER_AUDIENCE` | Application ID of a trusted caller (e.g. RCW API) whose forwarded `X-Authorization` token this API also accepts |

Then export the variables and run:

```bash
set -a && source .env.entra && set +a
./gradlew :info-and-advice-datastore-service:bootRun
```
## Run application locally using Bruno

Install bruno.

Setup a file `bruno-collections/Info and Advice Datastore API\environments\local.bru` this file is gitignored since it holds ever changing variables. Set the content to.
```
vars {
  baseUrl: http://localhost:8080
  oauthTokenUrl: http://host.docker.internal:9090/default/token
  clientId: test
  clientSecret: test
  token:
  applicationId:
}
```

When creating an application using StartApplication this will save the applicationId created to a variable, this is then reused for all PATCH/PUT/GET(singular).

The Get Token, endpoint will get a token from the mock 0auth2 service and automtically set it to both the `Authorization` and `X-Authorization` header for ease.


## Using the API Package

The `info-and-advice-datastore-api` module is published to GitHub Packages and can be consumed by other Java services to share request/response types and the API interface definition.

### What's included

- **Request types** : `StartApplicationCommand`, `UpdateMeansDataCommand`, `UpdateEvidenceCommand`, `DeclarationCommand`
- **Response types** : `ApplicationResponse`, `ApplicationResponses`, `ApplicationSummary`, `DeclarationResponse`, `EligibilityResultResponse`
- **Shared types** : `ApplicationState`, `ClientDeclarationStatus`, `ClientDetails`, `Address`
- **`ApplicationApi` interface** : can be used with a Feign client for a type-safe HTTP client with minimal boilerplate

### Adding the dependency

**Gradle:**
```gradle
implementation 'uk.gov.justice.laa.ia.datastore:info-and-advice-datastore-api:0.1.0'
```

**Maven:**
```xml
<dependency>
  <groupId>uk.gov.justice.laa.ia.datastore</groupId>
  <artifactId>info-and-advice-datastore-api</artifactId>
  <version>0.1.0</version>
</dependency>
```

### Repository configuration

GitHub Packages requires authentication even for public repositories. You'll need to add the repository to your build config alongside your credentials.

**Gradle** : add to your `repositories` block:
```gradle
maven {
    url = 'https://maven.pkg.github.com/ministryofjustice/laa-info-and-advice-datastore'
    credentials {
        username = System.getenv("GITHUB_ACTOR")?.trim() ?: project.findProperty('gitPackageUser')
        password = System.getenv("GITHUB_TOKEN")?.trim() ?: project.findProperty('gitPackageKey')
    }
}
```

**Maven** : add to your `pom.xml`:
```xml
<repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/ministryofjustice/laa-info-and-advice-datastore</url>
</repository>
```
With credentials in `~/.m2/settings.xml`:
```xml
<server>
    <id>github</id>
    <username>YOUR_GITHUB_USERNAME</username>
    <password>YOUR_GITHUB_PAT</password>
</server>
```

> **Note:** If your project uses `laa-spring-boot-gradle-plugin`, the credentials are likely already configured via `~/.gradle/gradle.properties` : see [Add GitHub Token](#add-github-token) above.

### Versioning

The API package is versioned independently of the service. The version is bumped manually in `info-and-advice-datastore-api/build.gradle` whenever the API contract changes. Check [GitHub Packages](https://github.com/ministryofjustice/laa-info-and-advice-datastore/packages) for the latest published version.

## Using the Client Package

The `info-and-advice-datastore-client` module is published to GitHub Packages and provides a ready-to-use, auto-configured HTTP client for calling this API from another Spring Boot service. It is generated from the same OpenAPI spec as the API package.

### What's included

- **Generated HTTP client** : `ApplicationApi` — a fully callable RestTemplate-backed client class for every endpoint
- **Model classes** : the same request/response types as the API package, under `uk.gov.justice.laa.ia.datastore.client.model`
- **Spring Boot autoconfiguration** : a `DatastoreApiClientConfiguration` that registers an `ApplicationApi` bean with both auth headers handled automatically

### Adding the dependency

**Gradle:**
```gradle
implementation 'uk.gov.justice.laa.ia.datastore:info-and-advice-datastore-client:0.1.0'
```

**Maven:**
```xml
<dependency>
  <groupId>uk.gov.justice.laa.ia.datastore</groupId>
  <artifactId>info-and-advice-datastore-client</artifactId>
  <version>0.1.0</version>
</dependency>
```

> See [Repository configuration](#repository-configuration) above — the same GitHub Packages setup applies.

### Consumer configuration

Add the following to your `application.yml`:

```yaml
laa:
  datastore:
    client:
      base-url: https://datastore.laa.gov.uk         # base URL of the datastore service
      client-registration-id: datastore              # matches a spring.security.oauth2.client.registration.* entry
```

The `client-registration-id` must correspond to an OAuth2 client registration configured in your app that is set up for a **client credentials** grant against Entra. For example:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          datastore:
            client-id: ${DATASTORE_CLIENT_ID}
            client-secret: ${DATASTORE_CLIENT_SECRET}
            authorization-grant-type: client_credentials
        provider:
          datastore:
            token-uri: https://login.microsoftonline.com/${TENANT_ID}/oauth2/v2.0/token
```

### Auth headers

The autoconfigured client attaches two Bearer tokens to every request automatically:

| Header | Source |
|---|---|
| `Authorization` | Acquired via the OAuth2 client credentials grant using `client-registration-id` — proves the calling service is trusted |
| `X-Authorization` | The JWT of the currently authenticated user, forwarded from the active Spring `SecurityContext` — must contain a `FIRM_CODE` claim |

The `X-Authorization` token is taken directly from the incoming request's security context, so it is propagated transparently as long as your service authenticates its own callers via Spring Security.

### Usage

Once the dependency and configuration are in place, inject `ApplicationApi` directly:

```java
@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final ApplicationApi datastoreApi;

    public ApplicationResponse getApplication(UUID id) {
        return datastoreApi.getApplication(id);
    }

    public ApplicationResponse startApplication(StartApplicationCommand command) {
        return datastoreApi.startApplication(command);
    }
}
```

No auth boilerplate is required at the call site — both tokens are handled by the client's `RestTemplate` interceptor.

### Versioning

The client package is versioned independently of both the service and the API package. The version is bumped manually in `info-and-advice-datastore-client/build.gradle` whenever the API contract changes. The client and API packages should be kept in sync with each other. Check [GitHub Packages](https://github.com/ministryofjustice/laa-info-and-advice-datastore/packages) for the latest published version.



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


