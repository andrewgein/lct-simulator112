FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /backend/app

COPY auth-service/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B

COPY auth-service/src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /backend/app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
