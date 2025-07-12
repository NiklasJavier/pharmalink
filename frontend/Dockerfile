FROM gradle:8.14.3-jdk24 AS builder

WORKDIR /app

COPY . .

RUN gradle build --no-daemon -x test

FROM eclipse-temurin:24-jre-ubi9-minimal

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]