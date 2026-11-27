# Build stage
FROM amazoncorretto:27.0.1-alpine@sha256:2a1963f0f2748128ca5ca81cc9ea69f26f1af4be481997afb04e8fb32f19aa4f AS builder

# Patch known CVEs in base image packages (e.g. zlib) ahead of a new upstream image tag
RUN apk update && apk upgrade --no-cache

RUN mkdir -p /build
WORKDIR /build

COPY gradlew settings.gradle build.gradle gradle.properties dependency-overrides.gradle ./
COPY gradle/ gradle/
COPY info-and-advice-datastore-api/ info-and-advice-datastore-api/
COPY info-and-advice-datastore-client/ info-and-advice-datastore-client/
COPY info-and-advice-datastore-service/ info-and-advice-datastore-service/

RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    chmod +x gradlew && ./gradlew :info-and-advice-datastore-service:bootJar --no-daemon

# Runtime stage
FROM ghcr.io/ministryofjustice/laa-base-container-images/java:25-corretto-alpine

USER root

# Set up working directory in the container
RUN mkdir -p /opt/laa-info-and-advice-datastore/
WORKDIR /opt/laa-info-and-advice-datastore/

# Copy the JAR file from the build stage
COPY --from=builder /build/info-and-advice-datastore-service/build/libs/info-and-advice-datastore-service-*.jar app.jar

# Amazon RDS root CAs so the JDBC connection can use sslmode=verify-full.
# Fetched at build time so the certificates are not stored in the repo.
RUN wget -q -O /opt/laa-info-and-advice-datastore/rds-ca-bundle.pem \
      https://truststore.pki.rds.amazonaws.com/eu-west-2/eu-west-2-bundle.pem \
    && grep -q "BEGIN CERTIFICATE" /opt/laa-info-and-advice-datastore/rds-ca-bundle.pem

# Set the default user
USER 1001

# Expose the port that the application will run on
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "app.jar"]