Cycle Route Planner Backend 🚲
This is the backend service for the Cycle Route Planner project, built with Java and Spring Boot. It provides optimized routing logic and integrates with advanced routing engines.

🔗 Useful Links
Task Management: Linear Board

Meetings: Google Meet Link

👥 Team Members
Oliver (@olivertiks) — DevOps & Backend

Lukas (@lukashaavel) — Team Lead & Backend

Natalia Egorova (@velesegorova12-code) — Documentation Specialist

Gretlin (@gretlin-prukk) — Frontend & Documentation

Anett (@anettagr) — Project Management

Raivo (@RaivoT) — Infrastructure Support

🛠 Tech Stack
Language: Java 21 (Spring Boot)

Containerization: Docker & Docker Compose

Build Tool: Maven

Routing Integration: BRouter / Digitransit (OTP2)

💻 Backend Technical Standards
🚀 Getting Started
To run the service locally, ensure you have Java 21 and Docker installed.

Build and Run

Bash
./mvnw clean install                # Build the project and install dependencies
docker compose up -d                # Start database and other services
./mvnw spring-boot:run              # Launch the Spring Boot application
🏗 Project Structure
src/main/java/ — Core application logic (Controllers, Services, Repositories)

src/main/resources/ — Configuration files and environment properties

src/test/java/ — Unit and integration tests

Dockerfile — Container configuration for production

compose.yaml — Docker Compose orchestration for local development

🎨 Development Guidelines
Code Style: Strict adherence to Java formatting standards (enforced via Maven plugins).

Architecture: Layered architecture focused on scalability and maintainability.

Documentation: All API endpoints should be documented and follow RESTful principles.
