# Multi-tenant Production Design (Blueprint)

This folder describes a **production-ready architecture** for Theratime, building on the current MVP:

- Schema-per-tenant (`tenant_1`, `tenant_2`, …)
- Outbox-based event publishing
- Notification-service consuming appointment events

The goal is to give a clear blueprint you can follow when taking the project live, without introducing half-implemented code into the main modules.

---

## 1. Key design goals

- **Strong tenant isolation**
  - One schema per tenant (practice).
  - Simple per-tenant backup/restore and data migration.

- **Reliable events**
  - No lost events when Kafka is down.
  - Idempotent, replayable consumers.

- **Resilient cross-service interactions**
  - Bounded latency and failure modes between services.

- **Incremental path from MVP to production**
  - MVP is fully functional now.
  - These steps can be added as you grow.

---

## 2. Dynamic tenant provisioning

### 2.1 Current approach

- Tenants `tenant_1` and `tenant_2` are created by Flyway migrations.
- Schemas and tables are “known ahead of time”.
- Good for a demo, but not enough for a real SaaS where tenants sign up dynamically.

### 2.2 Production approach

Introduce a **TenantProvisioningService** (could live in user-service or its own microservice).

**Responsibilities:**

1. **Create tenant metadata** (user-service):

   - Endpoint:

     POST /api/tenants
   - Behavior:
      - Insert a row into tenants table (e.g. name, contact, status).
      - Return the generated tenantId.

2. **Create tenant schema and tables** (appointments DB):
   - For each new tenant:
   ```CREATE SCHEMA tenant_<id>;     -- then run the same DDL as Flyway migrations for appointments, calendar_blocks, outbox```
   - Tables to create inside tenant_<id>:
      - appointments
      - calendar_blocks
      - outbox
      - Indexes/constraints (e.g. unique (therapist_id, start_time))

3. **Register tenant for outbox & background jobs:**
   - Maintain a tenant registry (e.g. user-service tenants table with status = ACTIVE).
   - OutboxPublisher and other tenant-scoped jobs read from this registry instead of a static list.
   Pseudo-code sketch:
   ```class TenantProvisioningService {    private final TenantRepository tenantRepository; // in user-service DB    private final JdbcTemplate adminJdbc;            // in appointments DB    public Long createTenant(TenantRequest req) {        Long tenantId = tenantRepository.insert(req.toEntity()); // create tenant metadata        provisionSchema(tenantId);        return tenantId;    }    private void provisionSchema(Long tenantId) {        String schema = "tenant_" + tenantId;        adminJdbc.execute("CREATE SCHEMA " + schema);        adminJdbc.execute(loadDdlFor("appointments"));      // CREATE TABLE tenant_<id>.appointments ...        adminJdbc.execute(loadDdlFor("calendar_blocks"));   // etc.        adminJdbc.execute(loadDdlFor("outbox"));            // etc.    }    private String loadDdlFor(String objectName) {        // Load DDL from a resource file or generator        // e.g. classpath:/db/ddl/appointments.sql    }}
   > In a real deployment, adminJdbc would use a connection with privileges to create schemas and run DDL in the appointments DB.
## 3. Tenant registry
   OutboxPublisher and other tenant-aware background jobs should not rely on hardcoded tenant IDs.

### 3.1 Design
   - Use the existing tenants table in user-service (or a dedicated registry table).
   - Expose an internal endpoint:
  ``` GET /api/tenants```
   returning active tenant IDs (and optional metadata).
   - In appointment-service, use a small client:
   ``` interface TenantRegistryClient {      List<Long> getActiveTenantIds();  } ```

### 3.2 OutboxPublisher integration
   Instead of parsing app.outbox.tenant-ids, OutboxPublisher would query the registry:
   ```class OutboxPublisher {    private final TenantRegistryClient tenantRegistry;    ...    @Scheduled(fixedDelayString = "${app.outbox.interval-ms:5000}")    void publishPending() {        for (Long tenantId : tenantRegistry.getActiveTenantIds()) {            try {                TenantContext.setTenantId(tenantId);                processTenantOutbox();            } finally {                TenantContext.clear();            }        }    }    private void processTenantOutbox() {        List<OutboxEntity> pending =            outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxEntity.STATUS_PENDING);        // same logic as in the MVP, but now for each discovered tenant    }}```

## 4. Transactional outbox (detailed)

### 4.1 Current shape
   - Outbox per tenant schema:
      - Each tenant schema (tenant_X) has an outbox table.
   - Enqueue:
      - OutboxService.enqueueEvent builds an AppointmentEventPayload, serializes it to JSON, and inserts a row with status = PENDING.
      - This is done in the same DB transaction as the appointment save.

### 4.2 Production refinements
   1. Backoff strategy
      Instead of retrying every fixed interval only, use exponential backoff:
```Duration baseDelay = Duration.ofSeconds(5);int attempt = row.getAttemptCount();Duration delay = baseDelay.multipliedBy((long) Math.pow(2, attempt));LocalDateTime nextAttemptAt = row.getCreatedAt().plus(delay);if (now.isBefore(nextAttemptAt)) {    // Too early to retry this row    return;}```
   2. Status lifecycle
   - PENDING: never tried or still retrying.
   - SENT: successfully published to Kafka.
   - FAILED: exceeded maxAttempts, should not be retried automatically.
   3. Outbox clean-up
   - Periodically clean up SENT rows older than N days (or archive them).
   - Keep a recent window for observability and debugging.

## 5. Kafka consumer robustness

### 5.1 Retry & DLQ with Spring Kafka
   To avoid losing events on transient failures in notification-service, use Spring Kafka support for retries and DLQs:
   ``` @RetryableTopic(  attempts = "3",  backoff = @Backoff(delay = 1000, multiplier = 2.0),  dltTopicSuffix = ".dlq")@KafkaListener(topics = "${app.kafka.topic.appointment-events}")public void consume(String message) {    ...}```
   - Transient errors are retried with backoff.
   - After max attempts, the message goes to appointment.events.dlq.

### 5.2 Idempotent consumer
   For safe retries or replays, implement an idempotency check:
   - Table:
      ```CREATE TABLE processed_events (      id VARCHAR(128) PRIMARY KEY,      tenant_id BIGINT NOT NULL,      processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP  );```
   - Handler logic:
      ```if (processedEventsRepository.existsById(eventId)) {      return; // already processed  }  notificationHandler.handle(payload);  processedEventsRepository.save(new ProcessedEvent(eventId, tenantId));```
   - eventId can be:
      - A hash of (tenantId, appointmentId, eventType, occurredAt), or
      - An explicit eventId field added to the event payload.

## 6. Cross-service resilience

### 6.1 Current pattern
   - appointment-service calls user-service via RestTemplate to:
   - Validate users and roles.
   - Resolve tenantId for TenantContext.

### 6.2 Production pattern
   Wrap these calls in timeouts and circuit breaker (e.g. Resilience4j):
   ``` class UserClient {    private final WebClient webClient;    private final CircuitBreaker cb;    Mono<User> getUserById(Long id) {        return cb.run(            () -> webClient.get()                    .uri("/user/{id}", id)                    .retrieve()                    .bodyToMono(User.class)                    .timeout(Duration.ofSeconds(2)),            throwable -> Mono.error(new UserServiceUnavailableException())        );    }}```

   - Avoids hanging threads when user-service is slow.
   - Avoids hammering a failing dependency.

## 7. SAGA (future option)

### 7.1 When you’d need it
   Introduce a SAGA when the business process involves multiple bounded contexts that must succeed or be compensated together, for example:
   - Book appointment
   - Charge payment
   - Sync to external CRM
   - Notify therapist and client
   If any step fails permanently, you may need to compensate earlier steps (e.g. refund payment, cancel appointment).

### 7.2 Why SAGA is not used in the current MVP
   For the MVP, SAGA is intentionally **not** implemented, even though the architecture is SAGA-ready:
   - **Appointments are the system of record**. Notifications (email/WhatsApp) and other downstream behaviors are **side-effects**; it is acceptable if they are occasionally delayed or, in rare edge cases, missed.
   - The combination of **transactional outbox in appointment-service**, **idempotent notification consumer with eventId**, **Kafka retries + DLQ**, and **cross-service retries/circuit breakers + timeouts** already gives strong at-least-once delivery and clear failure modes without orchestration complexity.
   - There is currently **no business requirement** that an appointment must be rolled back when a downstream step fails (e.g. payment, CRM sync). Without such an all-or-nothing requirement, SAGA mainly adds moving parts (orchestrator, compensating actions, saga state) without proportional benefit.
   - Keeping the MVP at this level avoids over-engineering and keeps operational and cognitive load low, while still providing a clear path to introduce SAGA later if business rules tighten.

### 7.3 How it would sit on top of current design
   - Use appointment events (appointment.created, appointment.cancelled, etc.) as saga triggers.
   - Introduce a BookingOrchestrator service:
      - Listens to events and drives the saga:
         - appointment.created → PaymentRequested → PaymentSucceeded → NotificationRequested.
      - On failure events:
         - Emit appointment.cancelled (compensating action).
   - The existing tenant-aware event model, transactional outbox, idempotent consumers, and Kafka topology are already compatible with a future SAGA orchestrator.