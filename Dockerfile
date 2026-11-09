# Build stage
FROM amazoncorretto:25-alpine@sha256:abed4fe0730b802823f0cbc5c24ab8e3ba6023af99266f0e2ca3c08d9f6a20f9 AS builder

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
FROM amazoncorretto:25-alpine@sha256:abed4fe0730b802823f0cbc5c24ab8e3ba6023af99266f0e2ca3c08d9f6a20f9

# Patch known CVEs in base image packages (e.g. zlib) ahead of a new upstream image tag
RUN apk update && apk upgrade --no-cache

# Set up working directory in the container
RUN mkdir -p /opt/laa-info-and-advice-datastore/
WORKDIR /opt/laa-info-and-advice-datastore/

# Copy the JAR file from the build stage
COPY --from=builder /build/info-and-advice-datastore-service/build/libs/info-and-advice-datastore-service-*.jar app.jar

# RDS root CAs so the JDBC connection can use sslmode=verify-full (see certs/README.md)
COPY certs/rds-eu-west-2-bundle.pem /opt/laa-info-and-advice-datastore/rds-ca-bundle.pem

# Create a group and non-root user
RUN addgroup -S appgroup && adduser -u 1001 -S appuser -G appgroup

# Set the default user
USER 1001

# Expose the port that the application will run on
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "app.jar"]