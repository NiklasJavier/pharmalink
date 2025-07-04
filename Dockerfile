FROM gradle:8.8-jdk21 AS build

WORKDIR /home/gradle/src

COPY . .

RUN chmod +x ./gradlew && ./gradlew build --no-daemon

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=dev
ENV SPRING_CONFIG_LOCATION=optional:file:/etc/pharmalink/application.yaml

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]