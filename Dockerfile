FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Cache Maven dependencies when only source changes
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline -DskipTests

COPY src ./src

RUN ./mvnw -q -B -DskipTests package

# DEVELOPMENT: Compose dev: bind-mount repo to /app, run ./mvnw spring-boot:run (needs JDK).
FROM eclipse-temurin:25-jdk AS development

WORKDIR /app

# PRODUCTION: Default image: JRE + fat JAR
FROM eclipse-temurin:25-jre AS production

WORKDIR /app

COPY --from=build /app/target/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
