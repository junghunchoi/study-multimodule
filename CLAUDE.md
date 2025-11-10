# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot multi-module Gradle project (loopers-java-spring-template) using Java 21. The project follows a three-tier module architecture designed for maintainability and reusability.

## Multi-Module Architecture

The project is organized into three distinct module types:

- **apps/** - Executable Spring Boot applications. Each module contains a `@SpringBootApplication` main class. These are the only modules with BootJar enabled.
  - `commerce-api` - REST API application with web endpoints
  - `commerce-streamer` - Kafka consumer application for event streaming

- **modules/** - Reusable, implementation-agnostic configuration modules. These provide infrastructure configurations without domain dependencies.
  - `jpa` - JPA/Hibernate configuration with QueryDSL support
  - `redis` - Redis configuration (master-replica setup)
  - `kafka` - Kafka configuration

- **supports/** - Add-on modules providing cross-cutting concerns and utility features.
  - `jackson` - JSON serialization configuration
  - `logging` - Logging configuration
  - `monitoring` - Prometheus & Grafana monitoring setup

## Build Commands

```bash
# Build entire project
./gradlew build

# Build specific module
./gradlew :apps:commerce-api:build

# Run tests for all modules
./gradlew test

# Run tests for specific module
./gradlew :apps:commerce-api:test

# Run single test class
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.example.ExampleServiceIntegrationTest"

# Run single test method
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.example.ExampleServiceIntegrationTest.Get.returnsExampleInfo_whenValidIdIsProvided"

# Generate JaCoCo coverage report
./gradlew test jacocoTestReport

# Clean build artifacts
./gradlew clean

# Run application (example for commerce-api)
./gradlew :apps:commerce-api:bootRun
```

## Local Development Environment

### Infrastructure Setup
Start required infrastructure (MySQL, Redis master-replica, Kafka with UI) using Docker Compose:
```bash
docker-compose -f ./docker/infra-compose.yml up
```

Services available:
- MySQL: `localhost:3306` (root/root, application/application)
- Redis Master: `localhost:6379`
- Redis Replica (read-only): `localhost:6380`
- Kafka: `localhost:9092` (container), `localhost:19092` (host)
- Kafka UI: `http://localhost:9099`

### Monitoring Setup
Start Prometheus and Grafana for local monitoring:
```bash
docker-compose -f ./docker/monitoring-compose.yml up
```

Access Grafana at `http://localhost:3000` (admin/admin).

## Code Architecture Patterns

### Application Layer Structure (commerce-api example)
The application follows a clean architecture pattern:

```
com.loopers/
├── domain/              # Business logic and domain models
│   └── example/
│       ├── ExampleModel.java        # Domain entity
│       ├── ExampleService.java      # Business logic
│       └── ExampleRepository.java   # Repository interface
├── application/         # Application facades/use cases
│   └── example/
│       ├── ExampleFacade.java       # Orchestrates domain services
│       └── ExampleInfo.java         # Application-level DTOs
├── infrastructure/      # Implementation of domain interfaces
│   └── example/
│       ├── ExampleJpaRepository.java    # Spring Data JPA interface
│       └── ExampleRepositoryImpl.java   # Repository implementation
├── interfaces/          # External interfaces (REST, consumers)
│   ├── api/
│   │   └── example/
│   │       ├── ExampleV1Api.java        # REST controller
│   │       └── ExampleV1Dto.java        # API DTOs
│   └── consumer/                         # Kafka consumers (in streamer app)
└── support/             # Application-level utilities
    └── error/
        ├── CoreException.java
        └── ErrorType.java
```

### Testing Strategy

The project uses three levels of testing:

1. **Unit Tests** - Test individual components (e.g., `ExampleModelTest.java`)
   - Pure logic testing without Spring context

2. **Integration Tests** - Test with Spring context and infrastructure (e.g., `ExampleServiceIntegrationTest.java`)
   - Annotated with `@SpringBootTest`
   - Uses Testcontainers for MySQL/Redis/Kafka
   - Cleans database using `DatabaseCleanUp.truncateAllTables()` in `@AfterEach`

3. **E2E/API Tests** - Full HTTP request/response testing (e.g., `ExampleV1ApiE2ETest.java`)
   - Uses `TestRestTemplate` with `SpringBootTest.WebEnvironment.RANDOM_PORT`
   - Tests actual REST endpoints

Test fixtures are provided in modules (e.g., `modules/jpa/src/testFixtures`) for reusable test utilities like:
- `DatabaseCleanUp` - Truncates all tables between tests
- `MySqlTestContainersConfig` - MySQL Testcontainer setup

### Configuration Management

- Each module exports its configuration via `src/main/resources/*.yml` files
- Applications import module configurations using `spring.config.import` in their `application.yml`
- Profiles: `local`, `test`, `dev`, `qa`, `prd`
- Timezone is set to `Asia/Seoul` globally

### Test Execution

Tests run with:
- `maxParallelForks = 1` (sequential execution)
- Timezone: `Asia/Seoul`
- Profile: `test`
- JVM args: `-Xshare:off`

## Key Dependencies

- Spring Boot 3.4.4
- Spring Cloud 2024.0.1
- Java 21
- QueryDSL (for type-safe queries)
- Lombok
- Testcontainers (MySQL, Redis, Kafka)
- SpringMockK, Mockito, Instancio (testing)
- SpringDoc OpenAPI (API documentation at `/swagger-ui.html`)

## HTTP Client Files

HTTP test files are located in `http/commerce-api/*.http` for manual API testing.

## Version Control

Project version is derived from Git commit hash using `getGitHash()` function if not explicitly set.

## CI/CD

GitHub Actions workflow (`.github/workflows/main.yml`) uses PR Agent for automated pull request reviews.
