# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Execução
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home valence && mkdir -p /app/data && chown valence /app/data
COPY --from=build /app/target/valence-api.jar app.jar
USER valence
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
