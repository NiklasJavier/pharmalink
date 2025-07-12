# Stage 1: Build the application with Gradle
# Verwendet das von dir angegebene JDK 24 Build-Image.
FROM gradle:8.14.3-jdk24 AS builder

WORKDIR /app

# Kopiert das gesamte Projektverzeichnis.
COPY . .

# Führt den 'build' Task aus.
# HINZUGEFÜGT: --stacktrace, um die genaue Gradle-Fehlermeldung zu sehen.
RUN gradle build --no-daemon -x test --stacktrace

# Benennt die JAR-Datei um.
RUN mv build/libs/*.jar app.jar


# Stage 2: Create the final, lean production image
# Verwendet das von dir angegebene JDK 24 Runtime-Image.
FROM eclipse-temurin:24-jre-ubi9-minimal

WORKDIR /app

# Kopiert NUR das gebaute JAR aus der Build-Stage.
COPY --from=builder /app/app.jar .

# Port, den die Anwendung nach außen freigibt
EXPOSE 8083

# Startet die Java-Anwendung
ENTRYPOINT ["java", "-jar", "app.jar"]