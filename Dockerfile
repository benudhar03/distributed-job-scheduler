FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy Maven configuration first for better Docker layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy application source
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B


FROM eclipse-temurin:21-jre-jammy

# Application metadata
LABEL org.opencontainers.image.title="AR Scheduling Service" \
      org.opencontainers.image.description="Scheduling service built with Spring Boot and Java 21" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.vendor="Ariyent Technologies" \
      org.opencontainers.image.authors="ar.techconsulting.in@gmail.com"

# Application directory
WORKDIR /app

# Create a dedicated non-root user
RUN groupadd --system spring && \
    useradd --system --gid spring --no-create-home spring

# Copy the built JAR
COPY --from=builder /build/target/ar-scheduling-service.jar /app/app.jar

# Make sure the application files are owned by the application user
RUN chown -R spring:spring /app

# Run as non-root user
USER spring

# Application port
EXPOSE 8080

# JVM configuration
ENV JAVA_OPTS="\
-XX:+UseContainerSupport \
-XX:MaxRAMPercentage=75.0 \
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-Djava.security.egd=file:/dev/./urandom"

# Spring Boot Actuator health check
HEALTHCHECK --interval=30s \
            --timeout=5s \
            --start-period=30s \
            --retries=3 \
            CMD wget --no-verbose --tries=1 \
                 --spider http://127.0.0.1:8080/actuator/health \
                 || exit 1

# Start application
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]