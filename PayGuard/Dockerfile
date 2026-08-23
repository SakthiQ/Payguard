# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Cache dependency layer first (only re-runs if pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build the fat JAR (skip tests — they run in CI)
COPY src ./src
RUN mvn package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S payguard && adduser -S payguard -G payguard
USER payguard

# Copy the fat JAR from builder stage
COPY --from=builder /app/target/PayGuard-0.0.1-SNAPSHOT.jar app.jar

# Expose the Spring Boot port
EXPOSE 8080

# JVM flags: reduce memory footprint in container, enable graceful shutdown
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
