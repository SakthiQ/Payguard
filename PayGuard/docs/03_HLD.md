# PayGuard — High-Level Design (HLD)

## 1. System Architecture Diagram

PayGuard is architected as a **Modular Monolith** with decoupled relational (MySQL) and document (MongoDB) persistence stores.

```text
               +----------------------------------+
               |   React Frontend / Postman API   |
               +----------------------------------+
                                |
                   HTTPS / REST API + HTTP-Only Cookie
                                |
               +----------------------------------+
               |      Spring Boot Application     |
               |                                  |
               |  [Spring Security & JWT Filter]  |
               |                  │               |
               |         [Web Controllers]        |
               |                  │               |
               |         [Service Layer]          |
               |   ┌──────────────┼─────────────┐ |
               |   │              │             │ |
               | Auth           Wallet       Payment  |
               | Service        Service      Engine   |
               |                  │             │ |
               |           [Fraud Engine]       │ |
               |                  │             │ |
               |       [Spring Event Publisher] │ |
               +------------------│-------------│-+
                                  │             │
                    Async Event   │             │ JPA / JDBC
                    (AFTER_COMMIT)│             │ (ACID)
                                  v             v
                           +-----------+   +----------+
                           |  MongoDB  |   |  MySQL   |
                           | Audit Logs|   | Main DB  |
                           +-----------+   +----------+
```

---

## 2. Component Breakdown & Responsibilities

### 2.1 Security Boundary
* **JWT Cookie Filter (`JwtAuthenticationFilter`)**: Intercepts requests, extracts JWT from `jwt` cookie, validates signature and expiration, populates `SecurityContextHolder`.
* **RBAC Enforcement**: Method-level authorization via Spring Security annotations (`@PreAuthorize`).

### 2.2 Payment Engine & Idempotency Layer
* **Idempotency Service**: Checks and locks `idempotency_keys` table. Atomically inserts `IN_PROGRESS` state. Returns cached response upon retries.
* **Transaction Service**: Coordinates balance validations, deterministic pessimistic locks, debiting sender, crediting receiver, and persisting `Transaction` records.

### 2.3 Fraud Engine Component
* **Rules Evaluator**: Evaluates incoming payments against active rule set (`HighAmountRule`, `VelocityRule`, `BlacklistRule`).
* **Fraud Queue Manager**: Routes flagged transactions to `fraud_flags` table for manual review by Fraud Analysts.

### 2.4 Asynchronous Audit Subsystem
* **Event Publisher**: Spring `ApplicationEventPublisher` publishes domain events after business operations.
* **Audit Event Listener**: `@TransactionalEventListener(phase = AFTER_COMMIT)` listens for domain events and persists JSON audit documents to MongoDB `audit_logs` collection.

---

## 3. Data Storage Responsibility Division

| Feature Area | Primary Storage | Secondary Storage | Rationale |
| :--- | :--- | :--- | :--- |
| **Users & Credentials** | MySQL (`users`) | - | Strong relational integrity, foreign keys |
| **Wallets & Balances** | MySQL (`wallets`) | - | ACID compliance, row-level locking |
| **Transactions & States** | MySQL (`transactions`) | - | Financial consistency, transactional status transitions |
| **Idempotency Keys** | MySQL (`idempotency_keys`) | - | Unique key constraint, relational transaction coupling |
| **Fraud Flags & Rules** | MySQL (`fraud_flags`, `fraud_rules`) | - | Relational joins with transactions & analyst users |
| **Audit Logs** | MongoDB (`audit_logs`) | - | Schemaless, append-only, high write throughput, immutable history |
