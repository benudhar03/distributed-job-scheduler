# Distributed Job Scheduler

A Spring Boot service for scheduling, tracking, and executing one-off and recurring background tasks across a distributed environment. Task state is persisted in MongoDB, and task lifecycle events are published/consumed via Kafka so execution can be coordinated across multiple running instances of the service.

---

## Use Case

Teams often need a central place to schedule work that shouldn't live inside a single monolith's cron jobs — things like:

- Triggering a batch job at a specific time (one-off) or on a recurring cadence (e.g. daily, hourly)
- Reliably firing a task exactly once even when multiple instances of the service are running (horizontal scaling)
- Tracking the status of each task (`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`) and querying by status
- Cancelling or manually triggering a task ahead of its schedule
- Decoupling "the thing that decides when to run" from "the thing that does the work" — the scheduler publishes an event to Kafka when a task is due, and a downstream consumer (in this service or another) does the actual execution and reports completion back

This service owns **scheduling and lifecycle management** of tasks. It does not necessarily perform the business logic of the task itself — that can be delegated to consumers listening on the relevant Kafka topics (see `dto/kafka` payloads: `TaskExecutionMessage`, `TaskCompletionMessage`, plus `EmailPayload`, `NotificationPayload`, `DataProcessingPayload` as example task-type payloads).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 4.1.1 (Spring Framework 7) |
| Web | Spring Web (REST) |
| Persistence | MongoDB (Spring Data MongoDB) |
| Messaging | Apache Kafka (Spring Kafka) |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| API Docs | springdoc-openapi (Swagger UI) |
| Monitoring | Spring Boot Actuator + Micrometer (Prometheus) |
| Boilerplate | Lombok |
| Build | Maven |
| Testing | JUnit 5, Spring Boot Test, Testcontainers (Mongo + Kafka), spring-kafka-test |

---

## Project Structure

```
com.ar.scheduling
├── config          # Mongo, Kafka, and OpenAPI bean configuration
├── controller       # REST endpoints (ArSchedulingController)
├── dto
│   ├── kafka         # Kafka message payloads (TaskExecutionMessage, TaskCompletionMessage, ...)
│   └── ...           # Request/response DTOs (ScheduleRequest, ScheduleResponse, ...)
├── entities          # MongoDB documents (ScheduledTask)
├── enums             # Task status / type enums
├── exception         # Custom exceptions (TaskNotFoundException, TaskValidationException)
├── health            # Custom health indicators
├── mapper            # Entity <-> DTO mapping
├── messaging         # Kafka producers/consumers
├── repository        # Spring Data MongoDB repositories
├── service           # Business logic (ArSchedulingService)
└── ArSchedulingServiceApplication.java
```

---

## API Endpoints

Base path: `/schedule`

| Method | Path | Description |
|---|---|---|
| `POST` | `/tasks` | Create (schedule) a new task |
| `GET` | `/tasks/{taskId}` | Get a task by ID |
| `GET` | `/tasks?page=&size=` | List all tasks (paginated) |
| `PUT` | `/tasks/{taskId}` | Update an existing scheduled task |
| `POST` | `/tasks/{taskId}/cancel` | Cancel a scheduled task |
| `POST` | `/tasks/{taskId}/trigger` | Immediately trigger a task's execution |
| `GET` | `/tasks/{taskId}/status` | Get current status of a task |
| `GET` | `/tasks/status/{status}?page=&size=` | List tasks filtered by status |
| `DELETE` | `/tasks/{taskId}` | Permanently delete a task |

Full request/response schemas are available via Swagger UI once the app is running (see below).

---

## Prerequisites

- Java 21
- Maven 3.9+
- A running MongoDB instance (local or remote)
- A running Kafka broker (local or remote)

---

## Configuration

Config lives in `src/main/resources/application.yml`. Key sections:

- `spring.data.mongodb.*` — Mongo host/port/database
- `spring.kafka.*` — bootstrap servers, producer/consumer serialization
- `management.*` — Actuator endpoint exposure (health, metrics, Prometheus)
- `springdoc.*` — Swagger UI path

Update the Mongo/Kafka connection details in `application.yml` to match your local or environment setup before running.

---

## Running Locally

```bash
# 1. Start MongoDB and Kafka (e.g. via Docker Compose, if available)

# 2. Build
mvn clean install

# 3. Run
mvn spring-boot:run
```

Once running:
- API base URL: `http://localhost:9080/schedule`
- Swagger UI: `http://localhost:9080/swagger-ui.html`
- Actuator health: `http://localhost:9080/actuator/health`
- Prometheus metrics: `http://localhost:9080/actuator/prometheus`

---

## Current Status

This project is under active debugging/setup. Known work in progress:

- [x] `pom.xml` reviewed and updated (added `spring-boot-starter-validation`, upgraded to Spring Boot 4.1.1)
- [x] `application.yml` populated (was empty) with Mongo, Kafka, Actuator, and springdoc config
- [ ] Verify `mvn clean compile` succeeds on Spring Boot 4.1.1 (springdoc / config-class compatibility to be confirmed)
- [ ] Confirm Mongo and Kafka connectivity on startup
- [ ] Verify each controller endpoint against the service/repository layer
- [ ] Confirm Kafka producer/consumer wiring for task execution events

---

## Workflow We're Following

1. Review each layer of the codebase (config → controller → service → repository → messaging) against the pom.xml dependencies to catch mismatches early.
2. Fix and verify one concern at a time (build, then startup, then endpoint behavior, then Kafka/Mongo integration) rather than changing everything at once.
3. After each change, run a build/startup check and share the resulting error or behavior before moving to the next fix.
4. Keep this README updated as the source of truth for setup steps and current status.