# Appointment events (Kafka)

Appointment-service publishes tenant-aware events to Kafka so other services (e.g. notifications, analytics) can react without coupling.

## Topic and key

- **Topic**: `appointment.events` (configurable via `app.kafka.topic.appointment-events`).
- **Message key**: `tenant_id` (string). Events for the same tenant go to the same partition, so ordering per tenant is preserved.

## Event types

| Event type             | When published        |
|-------------------------|------------------------|
| `appointment.created`   | After a new booking   |
| `appointment.cancelled` | After an appointment is cancelled |
| `appointment.rescheduled` | After an appointment is rescheduled |

## Payload shape (tenant-aware from the start)

All events use the same payload structure. `tenant_id` is always present so consumers can route or filter by tenant without future schema changes.

```json
{
  "eventType": "appointment.created",
  "tenantId": 1,
  "appointmentId": 42,
  "userId": 10,
  "therapistId": 5,
  "startTime": "2025-02-01T10:00:00.000Z",
  "endTime": "2025-02-01T10:45:00.000Z",
  "status": "BOOKED",
  "occurredAt": "2025-01-24T12:00:00.123Z"
}
```

- **eventType**: `appointment.created` | `appointment.cancelled` | `appointment.rescheduled`
- **tenantId**: Tenant (practice) id — always present.
- **appointmentId**, **userId**, **therapistId**: Domain ids.
- **startTime**, **endTime**: ISO-8601 (UTC).
- **status**: Appointment status at event time (e.g. `BOOKED`, `CANCELLED`).
- **occurredAt**: When the event was published (UTC).

## Reliable publishing: transactional outbox

Events are written to an **outbox table** in the same DB transaction as the appointment (per-tenant schema). A scheduled **OutboxPublisher** polls PENDING rows, publishes to Kafka, and marks SENT (or increments attempt and marks FAILED after `app.outbox.max-attempts`). Config: `app.outbox.tenant-ids`, `app.outbox.interval-ms`, `app.outbox.max-attempts`. No events are lost when Kafka is down; HTTP latency is not tied to Kafka.

## Concurrency: prevent double-booking

A **unique index** on `(therapist_id, start_time)` per tenant schema prevents two appointments for the same therapist at the same start time. Book and reschedule catch `DataIntegrityViolationException` and return 409 "Slot already taken". Tradeoff: slot is defined by exact start_time; overlapping ranges (different start, same slot) are still prevented by the existing overlap check.

## Tradeoffs

| Decision | Choice | Tradeoff |
|----------|--------|----------|
| **Topic strategy** | Single topic `appointment.events` with `eventType` in payload | One topic is simpler; key by `tenant_id` gives per-tenant ordering. Separate topics per event type would simplify consumer subscription but add more topics. |
| **Message key** | `tenant_id` | Same tenant → same partition → order preserved per tenant. Alternative: `appointment_id` for per-appointment ordering; we chose tenant for multi-tenant consumers. |
| **Publishing** | Transactional outbox + scheduled publisher | No lost events when Kafka is down; API does not depend on Kafka. Downside: extra table and background job; eventual delivery (delay up to `interval-ms`). |
| **Double-booking** | Unique (therapist_id, start_time) per schema | Race-free; DB enforces one appointment per slot. Slot = exact start_time; overlap check still prevents overlapping ranges. |
| **Payload** | JSON, flat, with `tenant_id` from day one | Consumers can filter by tenant and evolve without event shape changes. |

## Consumer notes

- **Idempotency**: Use `(appointmentId, eventType, occurredAt)` or a stored last-processed offset to deduplicate.
- **Tenant isolation**: Filter or partition by `tenantId`; key already ensures same-tenant order.
- **Schema**: No Avro/registry in this MVP; JSON is used. For schema evolution later, consider a schema registry and backward-compatible fields.
