# Stage 1: Build the application
FROM gradle:8.5.0-jdk17 AS build

WORKDIR /home/gradle/src

COPY . .

RUN chmod +x ./gradlew && ./gradlew build --no-daemon

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV SPRING_CONFIG_LOCATION=optional:file:/etc/pharmalink/config.yaml

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]