# Build stage
FROM gradle:8.10-jdk17 AS build
WORKDIR /build

# Copy protobuf module (needed for compilation)
COPY spaced-repetition-protobuf ./spaced-repetition-protobuf

# Copy data module sources and Gradle wrapper
COPY spaced-repetition-data/gradlew .
COPY spaced-repetition-data/gradle ./gradle
COPY spaced-repetition-data/build.gradle .
COPY spaced-repetition-data/src ./src

# Make gradlew executable
RUN chmod +x gradlew

# Fix proto source path in build.gradle (replace ../spaced-repetition-protobuf with spaced-repetition-protobuf)
RUN sed -i 's|\.\./spaced-repetition-protobuf|spaced-repetition-protobuf|g' build.gradle

# Diagnostic: list proto files to verify they exist
RUN echo "Checking proto files:" && find spaced-repetition-protobuf -name "*.proto" || echo "No proto files found"

# Generate protobuf classes explicitly
RUN ./gradlew generateProto

# Build the application (skip tests)
RUN ./gradlew clean build -x test

# Runtime stage
FROM amazoncorretto:17-alpine

ARG BUILD_TIME
RUN echo "Build timestamp: ${BUILD_TIME}" > /build-info.txt

# Install curl for healthcheck
RUN apk add --no-cache curl

WORKDIR /app
COPY --from=build /build/build/libs/data-service.jar app.jar

EXPOSE 9090

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=30s \
    CMD curl -f http://localhost:8081/health/ready || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]