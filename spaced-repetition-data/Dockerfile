# Dockerfile for Spaced Repetition Data Service
FROM amazoncorretto:17-alpine

ARG BUILD_TIME

# Install curl for healthcheck
RUN apk add --no-cache curl

WORKDIR /app

COPY spaced-repetition-data/build/libs/data-service.jar /app/data-service.jar

# JVM Configuration
ENV JAVA_OPTS="-Xmx256m -Xms64m -XX:+UseContainerSupport -XX:MaxRAMPercentage=50.0"

EXPOSE 9090

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=30s \
    CMD curl -f http://localhost:8081/health/ready || exit 1

ENTRYPOINT ["java", "-jar", "data-service.jar"]