## Production Operations Overview

This document summarizes how the Theratime system is intended to run in a production setting and which operational concerns are already addressed in the codebase.

### Deployment model

- **Services**: `auth-service`, `user-service`, `appointment-service`, `notification-service` run as independent Spring Boot applications (JARs or containers).
- **Recommended**: Containerize all services and deploy to:
  - A managed Kubernetes cluster (EKS/GKE/AKS) or
  - A Docker-based platform (ECS, Nomad, Docker Swarm) for smaller deployments.
- **Datastores & infra**:
  - PostgreSQL for each bounded context DB (auth, user, appointments, notification).
  - Kafka cluster for appointment events.
  - Optional Redis for session/caching.

### Configuration & environments

- Use Spring profiles (`application-dev.yml`, `application-prod.yml`) to separate:
  - DB URLs and credentials
  - Kafka/Redis endpoints
  - Logging verbosity
  - Resilience4j settings (retry counts, circuit breaker thresholds)
- Secrets (passwords, JWT secrets, SMTP credentials) should be provided via:
  - Environment variables, or
  - Secrets manager / KMS (e.g., AWS Secrets Manager, HashiCorp Vault), injected into Spring config.

### Observability

- **Metrics**:
  - Micrometer + Prometheus registry enabled in appointment-service and notification-service.
  - Suggested dashboards:
    - HTTP request rate/latency/error count per endpoint.
    - Outbox queue size and age of oldest pending event.
    - Kafka consumer lag and DLQ counts.
  - Alerting examples:
    - “Outbox oldest pending event age > N minutes”
    - “Notification DLQ messages increasing over time”

- **Logging**:
  - Use structured JSON logging in production (via logback encoder) to feed into ELK, Loki, or CloudWatch.
  - Log key business events at INFO (appointment booked/rescheduled/cancelled, notification sent).

- **Tracing**:
  - Add OpenTelemetry or Spring Cloud Sleuth to propagate correlation IDs across:
    - API gateway → auth-service → appointment-service → notification-service.
  - Export traces to Zipkin/Jaeger or a managed APM.

### Resiliency & failure handling

- **Cross-service HTTP calls**:
  - Timeouts and Resilience4j retries/circuit breakers are configured for:
    - `appointment-service` → `user-service` lookups.
    - `auth-service` → `user-service` registration.
  - Production guidance:
    - Use slightly lower retry counts and longer open-state durations than in dev.
    - Surface “dependency unavailable” as 503 with a clear message.

- **Kafka & outbox**:
  - Transactional outbox ensures appointment changes and events are committed atomically.
  - OutboxPublisher should be monitored (pending vs failed rows).
  - Notification-service uses `@RetryableTopic` with DLQ and idempotent consumer:
    - Investigate DLQ periodically, fix poison messages, and replay if needed.

- **Backups & recovery**:
  - PostgreSQL:
    - Regular logical backups (pg_dump) or snapshots.
    - For schema-per-tenant, allow per-tenant backup/restore by dumping individual schemas.
  - Kafka:
    - Appropriate retention for main topic and DLQ.
  - Verify disaster recovery by periodically restoring from backups in a non-prod environment.

### Runbooks & on-call basics

- Common runbook entries:
  - “User-service unavailable”: check circuit breaker metrics, logs; roll out or roll back deployments; verify DB health.
  - “Outbox stuck”: inspect outbox table for FAILED rows; fix root cause (Kafka, schema changes) and manually requeue if appropriate.
  - “Notification DLQ growth”: inspect payloads, correct bad config (e.g., missing tenant notification settings), replay after fix.

This document is intentionally concise; see `docs/PROD_READINESS.md` and `design/multi_tenant_production/README.md` for deeper architectural details and evolution plans.

