FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /backend

COPY shared/ shared/
COPY review-service/pom.xml app/pom.xml

WORKDIR /backend/app
RUN mvn dependency:go-offline -B

COPY review-service/ ./
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /backend/app/target/*.jar app.jar
EXPOSE 8085 9092
ENTRYPOINT ["java", "-jar", "app.jar"]
