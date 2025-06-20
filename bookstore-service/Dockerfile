FROM openjdk:17-jdk-slim

WORKDIR /app

# Copiază jar-ul aplicației
COPY target/*.jar app.jar

# Expune portul
EXPOSE 8080

# Rulează aplicația
ENTRYPOINT ["java", "-jar", "app.jar"]