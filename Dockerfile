FROM maven:3.9-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-jammy
COPY --from=build target/authentication-0.0.1-SNAPSHOT.jar authentication.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "authentication.jar"]