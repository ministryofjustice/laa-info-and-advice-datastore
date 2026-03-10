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
Once you've created your repository using this template, perform the following steps:

### Add Repository To Snyk (TODO)
Ensure that your repository has been added to the [Legal Aid Agency Snyk](https://app.snyk.io/org/legal-aid-agency) organisation.

Also add `SNYK_TOKEN` as a repository secret.

#### 5. Update GitHub workflow (TODO)
The following workflows have been provided:

* Build and test PR - `build-test-pr.yml`
* Build and deploy after PR merged - `pr-merge-main.yml` 

In the above workflow files, change all occurrences of the `spring-boot-microservice-service/build/` build path to `{application-name}-service/build/`.

</details>

### Add GitHub Token
Generate a Github PAT (Personal Access Token) to access the required plugin, via https://github.com/settings/tokens

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

### Run integration tests
`./gradlew integrationTest`

### Run application
`./gradlew bootRun`

### Run application via Docker
`docker compose up`

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


