FROM gradle:8.14.3-jdk24 AS builder
WORKDIR /app

COPY . .

RUN gradle build  -Pvaadin.productionMode=true --no-daemon -x test

RUN mv build/libs/pharmalink-client.jar app.jar

FROM eclipse-temurin:24-jre-ubi9-minimal
WORKDIR /app

COPY --from=builder /app/app.jar .

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar", "--vaadin.productionMode=true"]