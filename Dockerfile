FROM gradle:8.8-jdk21 AS build
WORKDIR /home/gradle/src

COPY build.gradle settings.gradle ./
RUN gradle bootJar --no-daemon -x test


RUN mv /home/gradle/src/build/libs/*.jar /home/gradle/src/app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /home/gradle/src/app.jar .

ENV SPRING_CONFIG_LOCATION=optional:file:/etc/pharmalink/application.yaml

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]