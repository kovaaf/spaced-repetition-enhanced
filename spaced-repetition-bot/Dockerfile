FROM maven:3.9.11-amazoncorretto-17 AS build

ARG SKIP_TESTS=false
ARG SKIP_CHECKSTYLE=false

WORKDIR /workspace

# Copy parent pom and checkstyle
COPY ./pom.xml .
COPY ./checkstyle.xml .

# Install parent pom to local repository (fix for missing parent artifact)
RUN mvn -N install

# Copy modules
COPY ./spaced-repetition-bot/ ./spaced-repetition-bot/
COPY ./spaced-repetition-protobuf/ ./spaced-repetition-protobuf/

# Build protobuf module
RUN if [ "$SKIP_CHECKSTYLE" = "true" ]; then \
        mvn -B -f ./spaced-repetition-protobuf/pom.xml clean install -Dcheckstyle.skip=true -DskipTests; \
    else \
        mvn -B -f ./spaced-repetition-protobuf/pom.xml clean install -DskipTests; \
    fi

# Build bot module
RUN if [ "$SKIP_CHECKSTYLE" = "true" ]; then \
        mvn -B -f ./spaced-repetition-bot/pom.xml clean package -Dcheckstyle.skip=true -DskipTests; \
    else \
        mvn -B -f ./spaced-repetition-bot/pom.xml clean package -DskipTests; \
    fi

# Test stage for running tests and generating reports
FROM build AS test
ARG SKIP_TESTS=false
ARG SKIP_CHECKSTYLE=false
WORKDIR /workspace

# Create directory for test reports
RUN mkdir -p /app/test-reports/coverage-reports \
    && mkdir -p /app/test-reports/test-results \
    && mkdir -p /app/test-reports/coverage-html

# Run tests and generate reports conditionally
RUN if [ "$SKIP_TESTS" = "true" ]; then \
        echo "Skipping tests as requested"; \
    elif [ "$SKIP_CHECKSTYLE" = "true" ]; then \
        mvn -B -f ./spaced-repetition-bot/pom.xml verify -Dcheckstyle.skip=true; \
    else \
        mvn -B -f ./spaced-repetition-bot/pom.xml verify; \
    fi

# Copy test reports to the test reports directory (only if tests were run)
RUN if [ "$SKIP_TESTS" != "true" ]; then \
        cp -r ./spaced-repetition-bot/target/site/jacoco/jacoco.csv /app/test-reports/coverage-reports/ 2>/dev/null || true; \
        cp -r ./spaced-repetition-bot/target/site/jacoco/jacoco.xml /app/test-reports/coverage-reports/ 2>/dev/null || true; \
        cp -r ./spaced-repetition-bot/target/surefire-reports/*.xml /app/test-reports/test-results/ 2>/dev/null || true; \
        cp -r ./spaced-repetition-bot/target/site/jacoco/*.html /app/test-reports/coverage-html/ 2>/dev/null || true; \
    fi

# Final image
FROM amazoncorretto:17-alpine

# Install curl for healthcheck
RUN apk add --no-cache curl

EXPOSE 8080
COPY --from=build /workspace/spaced-repetition-bot/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=120s \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]