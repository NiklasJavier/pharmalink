# Stage 1: Build the application with Gradle
FROM gradle:8.8-jdk21 AS build
WORKDIR /home/gradle/src

# Kopieren Sie das gesamte Projektverzeichnis in den Container.
# Das ist der einfachste und zuverlässigste Weg.
COPY . .

# Machen Sie den Gradle Wrapper ausführbar und führen Sie direkt 'bootJar' aus.
# 'bootJar' ist der spezifische Befehl von Spring Boot, um das ausführbare JAR zu erstellen.
# '--no-daemon' wird für CI/CD-Umgebungen wie Docker empfohlen.
RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon

# Jetzt sollte die JAR-Datei existieren. Benennen Sie sie um.
RUN mv build/libs/*.jar app.jar


# Stage 2: Create the final, lean production image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Die umbenannte JAR-Datei aus der Build-Stage kopieren
COPY --from=build /home/gradle/src/app.jar .

# Ihre Umgebungsvariable für die externe Konfiguration
ENV SPRING_CONFIG_LOCATION=optional:file:/etc/pharmalink/application.yaml

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]