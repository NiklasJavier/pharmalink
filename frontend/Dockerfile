# Stage 1: Build the application with Gradle
# Verwendet das von dir angegebene JDK 24 Build-Image.
FROM gradle:8.14.3-jdk24 AS builder

WORKDIR /app

# Kopiert den gesamten Projektkontext in das Build-Verzeichnis
COPY . .

# Führt den 'build' Task aus, um das Vaadin-Frontend und das Backend zu bauen.
# Mit '-x test' werden die Tests übersprungen, was den Build beschleunigt.
RUN gradle build --no-daemon -x test

# Benennt die JAR-Datei um, um den Prozess robust zu machen
RUN mv build/libs/*.jar app.jar


# Stage 2: Create the final, lean production image
# Verwendet das von dir angegebene JDK 24 Runtime-Image.
FROM eclipse-temurin:24-jre-ubi9-minimal

WORKDIR /app

# Kopiert NUR das gebaute JAR aus der Build-Stage
COPY --from=builder /app/app.jar .

# Port, den die Anwendung nach außen freigibt
EXPOSE 8083

# Startet die Java-Anwendung
ENTRYPOINT ["java", "-jar", "app.jar"]