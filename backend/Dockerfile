# Stage 1: build the Spring Boot JAR
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package \
    && JAR="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' ! -name '*-original.jar' | sort | head -n 1)" \
    && test -n "$JAR" \
    && cp "$JAR" /app/application.jar

# Stage 2: run the generated JAR
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/application.jar /app/application.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/application.jar"]