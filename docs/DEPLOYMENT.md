## Deployment Guide

This document describes how to run Theratime locally for development and how to approach a production-style deployment.

---

### 1. Components

- **Services**
  - `auth-service` (JWT authentication)
  - `user-service` (tenants & users)
  - `appointment-service` (booking, calendar, outbox, Kafka publisher)
  - `notification-service` (Kafka consumer, email/WhatsApp)

- **Infrastructure**
  - PostgreSQL (`authdb`, `userdb`, `appointmentsdb`, `notificationdb`)
  - Kafka + ZooKeeper
  - Zipkin (optional, for tracing)
  - (Optional) Prometheus / Grafana for metrics

---

### 2. Local deployment with Docker Compose

`docker-compose.yml` at the project root starts:

- `postgres` with databases initialized from `init/init.sql`
- `zookeeper`, `kafka`
- `zipkin`
- All four services, built from their `Dockerfile`s

#### 2.1 Build and start

```bash
# From repo root
mvn clean package -DskipTests

docker-compose build
docker-compose up
```

Services will be available at:

- Auth: `http://localhost:8083`
- User: `http://localhost:8082`
- Appointment: `http://localhost:8081`
- Notification: `http://localhost:8084`

Zipkin UI (if you enable tracing endpoints) is at `http://localhost:9411`.

#### 2.2 Environment alignment

The compose file wires environment variables so that:

- JDBC URLs point at the `postgres` container.
- Kafka bootstrap servers point at the `kafka` container.
- Spring profile `dev` is active by default.

If you change ports or DB names in `application-dev.yml`, update `docker-compose.yml` accordingly.

---

### 3. Kubernetes deployment (reference manifests)

The `k8s/` directory contains **minimal** manifests to illustrate a realistic deployment:

- `configmap.yaml` – shared config (e.g., Kafka and Zipkin endpoints)
- `<service>-deployment.yaml` – Deployment + Service per microservice
- `ingress.yaml` – Ingress routing to auth-service and appointment-service

#### 3.1 Images

Build and push images to your registry (example using Docker Hub):

```bash
mvn clean package -DskipTests

docker build -t your-dockerhub/theratime-auth-service:latest auth-service
docker build -t your-dockerhub/theratime-user-service:latest user-service
docker build -t your-dockerhub/theratime-appointment-service:latest appointment-service
docker build -t your-dockerhub/theratime-notification-service:latest notification-service

docker push your-dockerhub/theratime-auth-service:latest
docker push your-dockerhub/theratime-user-service:latest
docker push your-dockerhub/theratime-appointment-service:latest
docker push your-dockerhub/theratime-notification-service:latest
```

Then update `image:` fields in the manifests (`k8s/*-deployment.yaml`) to match your registry paths.

#### 3.2 Infra prerequisites

You need:

- A PostgreSQL instance or cluster reachable from the K8s cluster.
- A Kafka cluster (or managed Kafka service).
- Optionally, a Zipkin or Jaeger installation.

The provided manifests assume:

- Databases are reachable at `postgres:5432` (adjust `SPRING_DATASOURCE_URL` env vars otherwise).
- Kafka is reachable at `kafka:9092` (adjust env/ConfigMap for your setup).

#### 3.3 Apply manifests

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/auth-deployment.yaml
kubectl apply -f k8s/user-deployment.yaml
kubectl apply -f k8s/appointment-deployment.yaml
kubectl apply -f k8s/notification-deployment.yaml
kubectl apply -f k8s/ingress.yaml
```

You may also need:

- A PostgreSQL `StatefulSet` or external managed DB configuration.
- A Kafka `StatefulSet` or external managed Kafka configuration.

#### 3.4 Ingress & DNS

`k8s/ingress.yaml` configures:

- `theratime.local/auth` → auth-service
- `theratime.local/appointments` → appointment-service

For local clusters (e.g., kind/minikube), you can map `theratime.local` to the ingress controller’s IP in `/etc/hosts`.

---

### 4. Configuration and secrets

- **Spring profiles**
  - `application-dev.yml` – local/dev defaults.
  - Add `application-prod.yml` (not committed) for production specifics.

- **Secrets**
  - DB passwords, JWT secrets, SMTP credentials should be provided as:
    - K8s Secrets (mounted as env vars), or
    - Cloud secret managers (Vault, AWS Secrets Manager, etc.).

- **Resilience & observability**
  - Resilience4j (retries/circuit breakers) are configured in `application-*.yml`.
  - Actuator + Micrometer + Prometheus registry and Zipkin tracing endpoints are enabled in service configs.

---

### 5. CI/CD notes (hooks only)

The repo is CI/CD-ready but does not enforce a particular pipeline:

- Parent POM includes `maven-surefire-plugin`, `maven-failsafe-plugin`, and `jacoco-maven-plugin` setup.
- You can wire a pipeline roughly as:
  1. `mvn clean verify`
  2. Build and push Docker images.
  3. `kubectl apply -f k8s/` (or use Helm/Kustomize built on top of these manifests).

The focus of this repository is on **deployable application artifacts and configuration**, so a CI pipeline can be added on top without further code changes.

