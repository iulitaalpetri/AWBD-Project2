# Multi-stage Dockerfile pentru bookstore-service
FROM maven:3.8.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copiază pom.xml pentru cache Docker layers
COPY pom.xml .

# Download dependencies (pentru cache)
RUN mvn dependency:go-offline -B

# Copiază codul sursă
COPY src ./src

# Build aplicația
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:17-jdk-slim

WORKDIR /app

# Instalează curl pentru health check
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copiază jar-ul din stage-ul de build
COPY --from=build /app/target/*.jar app.jar

# Expune portul pentru bookstore-service
EXPOSE 8082

# Setează variabile de mediu
ENV SPRING_PROFILES_ACTIVE=docker

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# Rulează aplicația
ENTRYPOINT ["java", "-jar", "app.jar"]