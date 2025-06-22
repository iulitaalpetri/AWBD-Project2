# Dockerfile pentru auth-service și bookstore-service (același pentru ambele)
FROM maven:3.8.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copiază pom.xml pentru cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiază codul sursă și build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Instalează curl pentru health check
RUN apk add --no-cache curl

# Copiază jar-ul
COPY --from=build /app/target/*.jar app.jar

# Expune portul (va fi setat prin environment variable)
EXPOSE 8081
EXPOSE 8082

# Variabile de mediu
ENV SPRING_PROFILES_ACTIVE=docker

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Start aplicația
ENTRYPOINT ["java", "-jar", "app.jar"]