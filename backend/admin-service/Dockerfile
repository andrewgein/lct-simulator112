FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /backend

COPY admin-service/pom.xml app/pom.xml

WORKDIR /backend/app
RUN mvn dependency:go-offline -B

COPY admin-service/ ./
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# pg_dump (backups) and gpg (encrypting the secrets archive) are shelled out to from BackupService.
RUN apk add --no-cache postgresql16-client gnupg
COPY --from=build /backend/app/target/*.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
