FROM gradle:8.14.2-jdk21 AS builder

WORKDIR /app

COPY . .

RUN gradle bootJar --no-daemon

RUN mv build/libs/*.jar app.jar

FROM eclipse-temurin:21-jre-noble

WORKDIR /app

COPY --from=builder /app/app.jar .

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]