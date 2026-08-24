# Build stage
FROM amazoncorretto:25-alpine AS builder

RUN mkdir -p /build
WORKDIR /build

COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle/ gradle/
COPY info-and-advice-datastore-api/ info-and-advice-datastore-api/
COPY info-and-advice-datastore-client/ info-and-advice-datastore-client/
COPY info-and-advice-datastore-service/ info-and-advice-datastore-service/

RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    chmod +x gradlew && ./gradlew :info-and-advice-datastore-service:bootJar --no-daemon

# Runtime stage
FROM amazoncorretto:25-alpine

# Set up working directory in the container
RUN mkdir -p /opt/laa-info-and-advice-datastore/
WORKDIR /opt/laa-info-and-advice-datastore/

# Copy the JAR file from the build stage
COPY --from=builder /build/info-and-advice-datastore-service/build/libs/info-and-advice-datastore-service-*.jar app.jar

# Create a group and non-root user
RUN addgroup -S appgroup && adduser -u 1001 -S appuser -G appgroup

# Set the default user
USER 1001

# Expose the port that the application will run on
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "app.jar"]