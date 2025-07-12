# Stage 1: Build the application with Gradle
FROM gradle:8.8-jdk21 AS build
WORKDIR /home/gradle/src

# 1. Gradle-Wrapper- und Build-Dateien kopieren
# Diese ändern sich selten, daher werden Abhängigkeiten im Cache behalten.
COPY build.gradle settings.gradle ./
COPY gradlew ./
COPY gradle ./gradle/

# 2. Den Build mit dem Wrapper ausführen (ohne Quellcode)
# Dies lädt alle Abhängigkeiten herunter. Dieser Schritt wird nur wiederholt, wenn sich die Build-Dateien ändern.
RUN chmod +x ./gradlew && ./gradlew build --no-daemon -x build

# 3. Den Quellcode kopieren
# Da sich der Quellcode am häufigsten ändert, kommt dieser Schritt zuletzt.
COPY src ./src

# 4. Die Anwendung final bauen
RUN ./gradlew build --no-daemon

# 5. JAR-Datei umbenennen, um den "Unable to access jarfile"-Fehler zu vermeiden
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