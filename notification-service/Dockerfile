FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy full multi-module project so notification-service can be built with its parent
COPY .. /build

RUN mvn -pl notification-service -am clean package -DskipTests

FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /build/notification-service/target/*.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]

