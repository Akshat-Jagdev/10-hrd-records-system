# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 hrd
COPY --from=build /app/target/hrd-records-backend-1.0.0.jar app.jar
USER hrd
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
