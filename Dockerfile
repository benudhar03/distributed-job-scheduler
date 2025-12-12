# Use OpenJDK 21 base image
FROM openjdk:21-jdk-slim

# Set maintainer label
LABEL maintainer="ar.techconsulting.in@gmail.com"

# Set working directory
WORKDIR /app

# Install MongoDB client tools (optional, for debugging)
RUN apt-get update && \
    apt-get install -y mongodb-clients curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy the JAR file from target directory
COPY target/ar-scheduling-service.jar app.jar

# Create a non-root user to run the application
RUN groupadd -r spring && useradd -r -g spring spring
USER spring

# Expose the application port
EXPOSE 8080

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]