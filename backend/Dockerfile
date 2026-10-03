# Stage 1: build the Spring Boot JAR
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package \
    && cp "$(ls target/keystone-backend-*.jar | grep -vE 'plain|original')" /app/application.jar

# Stage 2: run the generated JAR
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/application.jar /app/application.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/application.jar"]
