# --- BUILD STAGE ---
FROM gradle:jdk21-alpine AS build

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle bootJar -x test

# --- RUNNING STAGE ---
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]