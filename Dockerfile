# 1. BASE: Shared setup for build and dev
FROM eclipse-temurin:25-jdk AS base
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
# Cache dependencies
RUN ./mvnw dependency:go-offline

# 2. DEVELOPMENT: This is what you'll use for hot reload
FROM base AS development
WORKDIR /app
# This command waits for the volume to be mounted, then runs.
CMD ["./mvnw", "spring-boot:run"]

# 3. BUILD: Compiles the JAR for production
FROM base AS build
COPY src ./src
RUN ./mvnw -q -B -DskipTests package

# 4. PRODUCTION: Final lean image
FROM eclipse-temurin:25-jre AS production
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
