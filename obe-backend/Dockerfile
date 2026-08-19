# ==========================================
# Build Stage
# ==========================================
FROM maven:3.9.8-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package application
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Runtime Stage
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Create non-root system user and persistent volume directories
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    mkdir -p /app/uploads /app/storage && \
    chown -R appuser:appgroup /app

# Copy application JAR from build stage
COPY --from=build --chown=appuser:appgroup /app/target/obe-backend-1.0.0.jar app.jar

# Run as non-root user
USER appuser

# Expose Spring Boot HTTP port
EXPOSE 8080

# Configure JVM flags and execution entrypoint
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
