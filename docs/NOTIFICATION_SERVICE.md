# Notification service

Consumes appointment events from Kafka and sends email/WhatsApp using **tenant-scoped** (and optional **per-therapist**) config.

## Config model: per-tenant + optional per-therapist

- **Chosen**: One table `notification_config` with `(tenant_id, therapist_id)`.
  - `therapist_id` **NULL** = tenant default (one default per tenant).
  - `therapist_id` **set** = override for that therapist within the tenant.
- **Lookup**: For event `(tenant_id, therapist_id)` → load config for `(tenant_id, therapist_id)` first; if missing, fallback to `(tenant_id, null)`.

This gives tenant-level defaults and per-therapist overrides from day one without extra tables or schema changes later.

## What it does

1. **Consumes** topic `appointment.events` (same as in [EVENTS.md](EVENTS.md)); payload includes `tenantId`, `therapistId`, `appointmentId`, `userId`, times, `eventType`.
2. **Resolves config** via `NotificationConfigResolver`: per-therapist then tenant default.
3. **Sends**:
   - **Email**: SMTP using tenant/therapist config (`email_from`, `smtp_host`, `smtp_port`, etc.). Implemented with `SmtpEmailSender` (builds `JavaMailSender` from config per send).
   - **WhatsApp**: Stub implementation that logs; wire a real client (e.g. Twilio, WhatsApp Business API) by implementing `WhatsAppSender` and `@ConditionalOnProperty(name = "app.whatsapp.stub", havingValue = "false")`.

Recipient for MVP: `default_to_email` and `default_to_phone` on config (e.g. practice admin). In production you would resolve recipient from user-service by `userId` (client email/phone).

## Config columns (tenant-scoped)

| Column | Purpose |
|--------|--------|
| `tenant_id`, `therapist_id` | Scope; `therapist_id` null = tenant default |
| `email_enabled`, `email_from`, `smtp_*` | Email channel |
| `whatsapp_enabled`, `whatsapp_phone_or_api_key` | WhatsApp channel |
| `default_to_email`, `default_to_phone` | Recipient for MVP (e.g. admin) |

## Tradeoffs

| Decision | Choice | Tradeoff |
|----------|--------|----------|
| **Config granularity** | Per-tenant default + optional per-therapist | One table, one lookup; therapist overrides without new tables. Alternative: tenant-only (simpler) but no per-therapist customization. |
| **Lookup order** | (tenant_id, therapist_id) then (tenant_id, null) | Therapist-specific branding/SMTP first; fallback to practice default. |
| **Email** | Build JavaMailSender from config per send | Each tenant can use different SMTP; no shared sender. Slight overhead per message. |
| **WhatsApp** | Stub by default; interface for real impl | No external API required for MVP; production implements `WhatsAppSender`. |
| **Recipient** | default_to_email / default_to_phone on config | MVP: single recipient per config. Later: resolve from user-service by userId. |

## Unrelated code

- No changes to appointment-service, auth-service, user-service, or event payload shape.
- Topic name remains `appointment.events`; consumer group is `notification-service-group`.
