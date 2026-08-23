# PayGuard — Software Requirements Specification (SRS)

## 1. System Requirements

### 1.1 Operating Environment & Tech Stack
* **Java Runtime**: OpenJDK 21 (LTS)
* **Framework**: Spring Boot 3.4.x / 3.5.x
* **Primary Relational Store**: MySQL 8.0 (ACID transactional business data)
* **Document Audit Store**: MongoDB 7.0 (Immutable event logs)
* **Schema Migration**: Flyway 10.x
* **Security & Auth**: Spring Security 6.x + JJWT (0.12.3) with HTTP-Only Cookies
* **Build System**: Apache Maven 3.9+
* **Deployment Orchestration**: Docker Compose v2.x

---

## 2. Detailed Functional Requirements (FR)

### FR-1: Authentication & Identity
* **FR-1.1**: The system must securely register users with `email`, `password`, `first_name`, `last_name`, and default role `ROLE_CUSTOMER`.
* **FR-1.2**: Passwords must be hashed using BCrypt (`PasswordEncoder`) with a strength of 12 before persistence.
* **FR-1.3**: Successful authentication must issue a signed JWT set in an `Set-Cookie` response header:
  - Header name: `Set-Cookie`
  - Cookie name: `jwt`
  - Flags: `HttpOnly`, `Secure` (in HTTPS/Prod), `SameSite=Strict`, `Path=/`
  - Expiration: 3600 seconds (1 hour).

### FR-2: Wallet & Financial Operations
* **FR-2.1**: Registration automatically provisions a primary `Wallet` with currency `USD` and balance `0.0000`.
* **FR-2.2**: The system must support simulated deposits (`POST /api/v1/wallets/{id}/deposit`) to increase wallet balance.
* **FR-2.3**: All balance modifications must execute within an active `@Transactional` database boundary.
* **FR-2.4**: Balance representation in Java must use `java.math.BigDecimal` (scale 4, `RoundingMode.HALF_EVEN`).

### FR-3: Payment Processing & Concurrency
* **FR-3.1**: Payment processing must accept `senderWalletId`, `recipientWalletNumber`, `amount`, and `description`.
* **FR-3.2**: Transfers require an `Idempotency-Key` HTTP header (UUID v4 format).
* **FR-3.3**: The system must lock sender and receiver wallet records using `SELECT FOR UPDATE`.
* **FR-3.4**: Lock acquisition order must sort wallet IDs numerically (`min(senderId, receiverId)` then `max(senderId, receiverId)`).

### FR-4: Fraud Engine & Review Queue
* **FR-4.1**: Transactions >= $5,000.00 trigger `HIGH_AMOUNT` fraud flag.
* **FR-4.2**: Sender attempting > 3 transfers within 300 seconds triggers `HIGH_VELOCITY` fraud flag.
* **FR-4.3**: Flagged transactions must enter state `FLAGGED` and populate `fraud_flags` table without debiting funds.
* **FR-4.4**: Fraud Analysts (`ROLE_FRAUD_ANALYST`) can execute `PUT /api/v1/fraud/flags/{id}/approve` or `decline`. Approval executes the transfer atomically; decline sets transaction status to `DECLINED`.

### FR-5: Audit Event System
* **FR-5.1**: All state-changing domain events (`USER_REGISTERED`, `WALLET_TOPUP`, `TRANSFER_INITIATED`, `TRANSFER_COMPLETED`, `TRANSFER_FLAGGED`, `FRAUD_APPROVED`, `FRAUD_DECLINED`, `WALLET_FROZEN`) must emit audit documents to MongoDB.
* **FR-5.2**: Audit emission must use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` to decouple audit execution from MySQL commit status.

---

## 3. Non-Functional Requirements (NFR)

### NFR-1: Reliability & Financial Safety
* Zero balance loss under high concurrency or simulated database connection drops.
* Unhandled runtime exceptions must return structured RFC 7807 problem responses without leaking internal stack traces.

### NFR-2: Performance & Scalability
* Payment transfer execution latency < 150ms under normal load.
* Idempotency check lookup latency < 15ms via database unique indexed query.

### NFR-3: Security Constraints
* No raw JPA entities exposed via REST controllers; 100% boundary mapping via DTOs and MapStruct/manual mappers.
* Method-level security enabled (`@PreAuthorize("hasRole('ADMIN')")`).
