# PayGuard — Project Development Plan

## 1. Project Definition

### Project Name

**PayGuard**

### Product

A **wallet-based payment processing platform with rule-based fraud detection, idempotent transaction processing, role-based security, and audit logging**.

### Primary Goal

Build a production-inspired fintech backend that demonstrates the engineering principles expected in Java/Spring fintech backend roles:

* Secure authentication and authorization
* Reliable financial transaction processing
* ACID transaction management
* Idempotency
* Fraud detection
* Transaction state management
* Auditability
* REST API design
* Database design
* Automated testing
* Containerized deployment

The React frontend will be developed after the backend is stable.

---

# 2. Product Scope

The system will have three primary actors.

### Customer

Can:

* Register and authenticate
* Manage wallet/account
* View balance
* Add beneficiaries
* Transfer money
* View transaction history
* View transaction status

### Fraud Analyst

Can:

* View flagged transactions
* Inspect fraud reasons
* Review transaction details
* Approve suspicious transactions
* Decline suspicious transactions
* Add investigation notes

### Administrator

Can:

* Manage users
* Freeze/unfreeze accounts
* Manage fraud rules
* Manage blacklisted accounts
* View operational statistics
* Manage system configuration

---

# 3. Core Transaction Flow

The central workflow of PayGuard is:

```text
Customer
    |
    v
Authenticate with JWT
    |
    v
Submit Transfer
    |
    v
Validate Request
    |
    v
Verify Account Ownership
    |
    v
Check Balance
    |
    v
Check Idempotency
    |
    v
Run Fraud Rules
    |
    +--------------------+
    |                    |
    v                    v
LOW RISK              SUSPICIOUS
    |                    |
    v                    v
Process Transfer     FLAGGED
    |                    |
    v                    v
Debit Sender        Fraud Queue
    |                    |
    v                    v
Credit Receiver     Analyst Review
    |                    |
    v              +-----+-----+
Transaction        |           |
Completed       Approve     Decline
```

This workflow is the core of the project and should drive the architecture, database design, APIs, tests, and frontend.

---

# 4. Technology Stack

## Backend

* Java 21
* Spring Boot
* Spring Web
* Spring Security
* Spring Data JPA
* Spring Data MongoDB
* Bean Validation
* Maven
* Lombok
* MapStruct

## Database

### MySQL

Responsible for transactional business data:

* Users
* Wallets/accounts
* Beneficiaries
* Transactions
* Fraud flags
* Idempotency records

### MongoDB

Responsible for audit/event documents:

* Authentication events
* Transaction events
* Fraud decisions
* Administrative actions
* Security events

## Database Migration

**Flyway**

All MySQL schema changes must be version controlled through Flyway migrations.

Hibernate must not be responsible for production schema creation.

## Security

* Spring Security
* JWT
* BCrypt/PasswordEncoder
* RBAC
* Input validation

## Frontend

Developed after backend completion:

* React
* TypeScript
* Vite
* Tailwind CSS or Material UI
* Axios
* React Router

## Infrastructure

* Docker
* Docker Compose
* Git
* GitHub

## API Testing

* Postman
* MockMvc
* Integration testing
* Testcontainers where appropriate

---

# 5. Architecture Strategy

The initial system should be a **modular monolith**, not microservices.

```text
                    React
                      |
                      v
                 REST API
                      |
             Spring Boot Application
                      |
     +----------------+----------------+
     |                |                |
 Authentication   Transaction       Fraud
     |             Processing       Engine
     |                |                |
     +----------------+----------------+
                      |
                Persistence Layer
                 /            \
                v              v
             MySQL          MongoDB
          Business Data     Audit Data
```

The architecture should have clear module boundaries so that individual modules could be extracted into microservices later.

Do not introduce Spring Cloud, Kafka, Redis, or an API Gateway into the initial implementation unless they become justified by a concrete requirement.

---

# 6. Context Engineering Package

Before asking Antigravity to implement the system, create a dedicated context repository.

```text
payguard-context/
│
├── 00_MASTER_CONTEXT.md
├── 01_PROJECT_OVERVIEW.md
├── 02_DOMAIN_MODEL.md
├── 03_PRODUCT_REQUIREMENTS.md
├── 04_SRS.md
├── 05_HLD.md
├── 06_LLD.md
├── 07_DATABASE_DESIGN.md
├── 08_API_CONTRACTS.md
├── 09_SECURITY_ARCHITECTURE.md
├── 10_BUSINESS_WORKFLOWS.md
├── 11_STATE_MACHINES.md
├── 12_CODING_STANDARDS.md
├── 13_ARCHITECTURE_DECISIONS.md
├── 14_TESTING_STRATEGY.md
├── 15_DEPLOYMENT_ARCHITECTURE.md
└── 16_IMPLEMENTATION_PLAN.md
```

These documents are the implementation context.

The objective is to minimize architectural decisions that Antigravity has to invent.

---

# 7. Context Hierarchy

The documents should have an explicit authority hierarchy:

```text
Architecture Decision Records
            |
           LLD
            |
           HLD
            |
           SRS
            |
           PRD
```

If two documents conflict, the higher-level authoritative decision wins and the agent must report the conflict rather than silently choosing.

---

# 8. Master Context

`00_MASTER_CONTEXT.md` defines how Antigravity operates.

It should establish:

* Agent role
* Engineering standards
* Approved technologies
* Architecture principles
* Coding conventions
* Security requirements
* Testing requirements
* Documentation requirements
* Prohibited shortcuts
* Context hierarchy
* Conflict resolution
* Definition of done

Important constraints include:

* No unnecessary technologies.
* No undocumented architecture changes.
* No entity exposure through APIs.
* DTOs for API boundaries.
* Constructor injection.
* Centralized exception handling.
* Input validation.
* Transactional boundaries explicitly defined.
* Financial operations must be atomic.
* Payment endpoints must support idempotency.
* Security must be enforced server-side.
* Business rules must remain in the backend.
* Every important state-changing operation must produce an audit event.

---

# 9. Product Requirements

Define exactly what the product does before technical implementation.

Major capabilities:

```text
Identity
    |
User Management
    |
Wallet Management & Deposit / Top-up (Sandbox Funding)
    |
Beneficiaries
    |
Payment Processing
    |
Fraud Detection
    |
Fraud Review
    |
Audit
    |
Administration
```

Each feature receives:

* Purpose
* Actors
* Preconditions
* Main flow
* Alternate flows
* Business rules
* Acceptance criteria

---

# 10. SRS

The SRS becomes the formal functional specification.

It should contain:

### Functional Requirements

Examples:

* User registration
* Authentication
* Wallet creation
* Balance retrieval
* Beneficiary management
* Money transfer
* Transaction history
* Fraud detection
* Fraud review
* Account freezing
* Audit logging

### Non-Functional Requirements

* Security
* Reliability
* Data integrity
* Performance
* Maintainability
* Observability
* Auditability
* Scalability

### Constraints

* No real banking integrations initially.
* No real money movement.
* No card storage.
* No production financial credentials.
* System operates as a controlled wallet simulation.

---

# 11. HLD

The HLD should define:

* System architecture
* Component responsibilities
* Data flow
* Technology choices
* Database responsibilities
* Security boundary
* Deployment architecture
* Scalability strategy

Initial architecture:

```text
                 Client
                   |
                   v
              REST API
                   |
           Spring Security
                   |
             Controllers
                   |
              Services
                   |
       +-----------+-----------+
       |           |           |
    Wallet    Transaction    Fraud
       |           |           |
       +-----------+-----------+
                   |
             Repositories
              /         \
             /           \
          MySQL        MongoDB
```

---

# 12. LLD

The LLD defines implementation structure.

Recommended package-by-feature organization:

```text
com.payguard
│
├── auth
├── user
├── wallet
├── beneficiary
├── transaction
├── fraud
├── audit
├── admin
├── security
├── config
├── common
└── exception
```

Each feature can contain its own:

```text
controller
service
repository
entity
dto
mapper
exception
```

The LLD should define the responsibility of every important class and interface before implementation.

---

# 13. Domain Model

Core entities:

```text
User
 |
 +---- Wallet
 |
 +---- Beneficiary
 |
 +---- Transaction
          |
          +---- FraudFlag
```

Additional supporting concepts:

* IdempotencyKey
* AuditEvent
* FraudRule
* Role
* AccountStatus
* TransactionStatus

---

# 14. Database Design

## MySQL

Primary transactional store.

Tables should include at minimum:

```text
users
wallets
beneficiaries
transactions
fraud_flags
idempotency_keys
fraud_rules
```

The database specification must define:

* Columns
* Types
* Primary keys
* Foreign keys
* Unique constraints
* Check constraints where supported
* Indexes
* Relationships
* Query patterns
* Migration order

### Monetary Data Precision Standard

* All financial balance and transfer amount fields in MySQL **MUST** use `DECIMAL(19,4)` or integer subunits (`BIGINT` in cents).
* Floating point types (`float`, `double`) are **strictly prohibited** in entities, DTOs, and database tables to prevent binary floating-point rounding errors.
* In Java code, monetary amounts **MUST** use `java.math.BigDecimal` with explicit rounding modes (`RoundingMode.HALF_EVEN` / Banker's Rounding).

## MongoDB

Collection:

```text
audit_logs
```

Audit documents should contain enough information to reconstruct significant system events without becoming the source of truth for financial balances.

---

# 15. Transaction Processing Design

Money movement must be treated as a critical transactional operation.

The implementation must establish a clear transaction boundary around:

```text
Validate
    |
Lock/verify required records
    |
Debit sender
    |
Credit receiver
    |
Persist transaction
    |
Persist idempotency record
```

Failure anywhere in the transaction must not leave partially updated financial state.

The design should explicitly address:

* Concurrent transfers
* Insufficient balance
* Duplicate requests
* Account status
* Deadlocks
* Transaction rollback
* Isolation
* Race conditions

### Concurrency & Deadlock Prevention Protocol

To guarantee consistency during concurrent peer-to-peer transfers (e.g. User A transferring to User B while User B transfers to User A simultaneously):

* **Pessimistic Locking**: Use `SELECT ... FOR UPDATE` on both sender and receiver wallet records inside `@Transactional` boundaries.
* **Deterministic Lock Ordering**: Locks MUST be acquired in a deterministic order by sorting target wallet IDs: `min(senderWalletId, receiverWalletId)` is locked first, followed by `max(senderWalletId, receiverWalletId)`. This completely eliminates circular wait conditions and database deadlocks.

---

# 16. Idempotency

Every transfer request should carry an idempotency key.

Example:

```http
Idempotency-Key: 7f2a...
```

The system must distinguish:

```text
First request
    |
Process payment
    |
Store result
```

from:

```text
Retry with same key
    |
Return previous result
    |
Do not execute payment again
```

The specification must define:

* Key format
* Uniqueness
* Storage
* Expiration/retention policy
* Duplicate behavior
* Conflict behavior

### Idempotency Key Lifecycle & Race Condition Protocol

* **Lifecycle States**: `IN_PROGRESS`, `COMPLETED`, `FAILED`.
* **Atomic Lock Insertion**: Upon receiving a transfer request, atomically insert an `IdempotencyKey` record with status `IN_PROGRESS` (enforced by a MySQL `UNIQUE` index on `idempotency_key`).
* **Concurrent Duplicate Requests**: If a 2nd request arrives with the same key while status is `IN_PROGRESS`, immediately reject with `409 Conflict` (or `429`).
* **Cached Completion**: When the transfer transaction succeeds, update state to `COMPLETED` along with the serialized HTTP response body and status code. Subsequent retries return the cached response immediately without re-executing logic.

---

# 17. Fraud Engine

Initial fraud detection should remain deterministic and explainable.

Rules:

### Velocity Rule

Detect excessive transaction frequency.

### High Amount Rule

Flag transactions above a configurable threshold.

### Blacklist Rule

Reject or flag transactions involving blocked accounts.

The fraud engine should produce:

```text
Fraud decision
Risk score
Triggered rules
Reason
Recommended action
```

Do not introduce machine learning into V1.

A deterministic rules engine is easier to test and gives you a strong foundation for a future ML-based risk engine.

---

# 18. Transaction State Machine

Define explicit legal states.

Example:

```text
CREATED
   |
VALIDATING
   |
   +------> FLAGGED
   |           |
   |       UNDER_REVIEW
   |         /      \
   |    APPROVED   DECLINED
   |       |
   v       |
APPROVED <-+
   |
COMPLETED
```

Invalid state transitions must be rejected.

---

# 19. Security Architecture

Implement:

```text
Login
  |
Credentials
  |
Spring Security
  |
Password Verification
  |
JWT Generation
  |
Authenticated API Request
  |
JWT Filter
  |
Authentication Context
  |
RBAC
  |
Controller
```

Roles:

```text
CUSTOMER
FRAUD_ANALYST
ADMIN
```

Authorization must be enforced on the backend.

The frontend must never be trusted for access control.

---

# 20. API Specification

Define all APIs before implementation.

Major groups:

### Authentication

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### Wallet

```text
GET /api/v1/wallets
GET /api/v1/wallets/{id}
GET /api/v1/wallets/{id}/balance
POST /api/v1/wallets/{id}/deposit
```

### Beneficiary

```text
POST /api/v1/beneficiaries
GET /api/v1/beneficiaries
DELETE /api/v1/beneficiaries/{id}
```

### Transactions

```text
POST /api/v1/transactions
GET /api/v1/transactions/{id}
GET /api/v1/transactions
```

### Fraud

```text
GET /api/v1/fraud/flags
GET /api/v1/fraud/flags/{id}
PUT /api/v1/fraud/flags/{id}/approve
PUT /api/v1/fraud/flags/{id}/decline
```

### Administration

```text
GET /api/v1/admin/users
PUT /api/v1/admin/wallets/{id}/freeze
PUT /api/v1/admin/wallets/{id}/unfreeze
```

Every endpoint specification must define:

* Authentication
* Authorization
* Request
* Response
* Validation
* Status codes
* Business rules
* Error conditions

---

# 21. Error Handling

Use centralized exception handling.

Define categories such as:

```text
ValidationException
AuthenticationException
AuthorizationException
ResourceNotFoundException
InsufficientBalanceException
DuplicateTransactionException
InvalidTransactionStateException
FraudException
BusinessException
```

Use consistent error responses.

---

# 22. Audit Architecture

Audit events should be generated for important state changes.

Examples:

```text
USER_REGISTERED
USER_LOGIN
WALLET_CREATED
TRANSFER_INITIATED
TRANSFER_COMPLETED
TRANSFER_FLAGGED
FRAUD_APPROVED
FRAUD_DECLINED
WALLET_FROZEN
```

Audit logging must not become a mechanism for bypassing transactional integrity.

### Asynchronous Dual-DB Isolation Protocol

* Audit event emission to MongoDB MUST be decoupled from MySQL financial transactions using Spring `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` or asynchronous event dispatching (`@Async`).
* A failure in MongoDB audit logging MUST NOT roll back a committed MySQL transaction nor return an error status to the API consumer. Audit operations must log alerts gracefully.

---

# 23. Testing Strategy

Backend development must be completed and verified before React development.

Testing layers:

```text
Unit Tests
    |
Service Tests
    |
Repository Tests
    |
Integration Tests
    |
API Tests
    |
End-to-End Tests
```

Critical scenarios:

* Successful transfer
* Insufficient balance
* Invalid receiver
* Unauthorized transfer
* Duplicate transfer
* Concurrent transfer
* Fraud flag
* Fraud approval
* Fraud rejection
* Frozen account
* Invalid JWT
* Invalid role
* Database rollback

---

# 24. Backend Verification

Postman acts as the initial client.

Example:

```text
Register
    |
Login
    |
Create Wallet
    |
Create Second User
    |
Create Second Wallet
    |
Transfer
    |
Check Transaction
    |
Check Balances
    |
Check Idempotency
    |
Trigger Fraud
    |
Review Fraud
    |
Check Audit Log
```

Every critical API should have an explicit verification scenario.

The project should maintain a backend verification matrix containing:

* Request
* Expected response
* Expected database state
* Expected audit event
* Expected side effects
* Failure behavior

---

# 25. Frontend Phase

React starts only after the backend passes its verification criteria.

Customer interface:

```text
Dashboard
Wallet
Beneficiaries
Transfer
Transactions
Profile
```

Fraud analyst interface:

```text
Fraud Dashboard
Flagged Transactions
Transaction Details
Approve
Decline
Investigation Notes
```

Admin interface:

```text
Users
Wallets
Fraud Rules
Blacklisted Accounts
System Statistics
```

The frontend should consume the backend APIs rather than duplicate business logic.

---

# 26. Deployment

Development environment:

```text
Docker Compose
│
├── Spring Boot
├── MySQL
└── MongoDB
```

Later:

```text
React
   |
Reverse Proxy
   |
Spring Boot
   |
+-- MySQL
|
+-- MongoDB
```

Environment-specific configuration should use environment variables rather than hardcoded credentials.

---

# 27. Implementation Phases

## Phase 0 — Specification

Complete:

* Project definition
* PRD
* SRS
* HLD
* LLD
* Database design
* API contracts
* Security architecture
* ADRs

No application code yet.

---

## Phase 1 — Project Bootstrap

Antigravity creates:

* Spring Boot project
* Maven configuration
* Package structure
* Configuration management
* Docker Compose
* Database connections
* Flyway
* Basic application health check

---

## Phase 2 — Persistence

Implement:

* Entities
* Repositories
* Flyway migrations
* Database constraints
* MongoDB audit infrastructure

Validate database behavior before proceeding.

---

## Phase 3 — Authentication

Implement:

* Registration
* Password hashing
* Login
* JWT
* Spring Security
* RBAC
* Authentication error handling

Test independently.

---

## Phase 4 — Wallet Management

Implement:

* Wallet creation
* Wallet retrieval
* Balance retrieval
* Sandbox wallet deposit / top-up (`POST /api/v1/wallets/{id}/deposit`)
* Wallet status
* Account ownership

Test independently.

---

## Phase 5 — Beneficiaries

Implement:

* Add beneficiary
* List beneficiaries
* Delete beneficiary
* Ownership validation

---

## Phase 6 — Payment Processing

Implement:

* Transfer request
* Validation
* Balance verification
* Transaction management
* Concurrency handling
* Idempotency
* Transaction states

This is the most important backend phase.

---

## Phase 7 — Fraud Engine

Implement:

* Rule engine
* Velocity detection
* Amount threshold
* Blacklist detection
* Fraud score
* Fraud flags

---

## Phase 8 — Fraud Review

Implement:

* Analyst queue
* Transaction inspection
* Approval
* Decline
* Review notes
* State transitions
* Audit events

---

## Phase 9 — Audit

Implement:

* Audit event model
* MongoDB persistence
* Security events
* Transaction events
* Administrative events

---

## Phase 10 — Hardening

Review:

* Security
* Validation
* Error handling
* Logging
* Concurrency
* Database indexes
* Transaction boundaries
* API consistency

---

## Phase 11 — Testing

Build:

* Unit tests
* Integration tests
* Repository tests
* API tests
* Concurrency tests
* Security tests

No frontend until the backend passes this phase.

---

## Phase 12 — Backend Acceptance

Run the complete verification matrix.

The backend is considered complete only when:

* All critical APIs work.
* Database state is correct.
* Transactions are atomic.
* Duplicate payments are prevented.
* Fraud rules behave correctly.
* RBAC works.
* Audit events are generated.
* Automated tests pass.
* Docker environment starts successfully.

---

## Phase 13 — React Frontend

Build:

* Authentication UI
* Customer dashboard
* Wallet UI
* Transfer UI
* Transaction history
* Fraud analyst dashboard
* Admin dashboard

---

## Phase 14 — Full-System Testing

Test:

```text
React
  |
REST API
  |
Spring Security
  |
Business Logic
  |
MySQL
  |
MongoDB
```

Validate complete user journeys.

---

# 28. Documentation Deliverables

The finished project should contain:

```text
README.md

docs/
│
├── PRD.md
├── SRS.md
├── HLD.md
├── LLD.md
├── DATABASE.md
├── API.md
├── SECURITY.md
├── WORKFLOWS.md
├── TESTING.md
├── DEPLOYMENT.md
└── ADR/
```

Architecture diagrams should include:

* System architecture
* Component diagram
* ER diagram
* Authentication sequence
* Payment sequence
* Fraud sequence
* Deployment diagram
* Transaction state machine

---

# 29. Definition of Done

PayGuard is not "done" because the application compiles.

It is done when:

```text
Architecture
     ✓

Database
     ✓

Security
     ✓

APIs
     ✓

Payment Processing
     ✓

Idempotency
     ✓

Fraud Detection
     ✓

Audit
     ✓

Testing
     ✓

Docker
     ✓

Backend Verification
     ✓

Frontend
     ✓

End-to-End Testing
     ✓

Documentation
     ✓
```

The final system should be something you can run locally, demonstrate through the React application, inspect through Postman, inspect directly at the database level, and explain at the architecture level during a backend interview.

---

# 30. Final Development Philosophy

The project should follow this sequence:

```text
Define the Product
        ↓
Define Requirements
        ↓
Design Architecture
        ↓
Design Database
        ↓
Design APIs
        ↓
Design Security
        ↓
Define Workflows
        ↓
Define Implementation Rules
        ↓
Give Context to Antigravity
        ↓
Generate Backend
        ↓
Test Backend
        ↓
Harden Backend
        ↓
Build Frontend
        ↓
End-to-End Testing
        ↓
Document
        ↓
Deploy
```

The critical principle is:

**Antigravity should implement the architecture, not invent the architecture.**

The purpose of the context-engineering work is to make the coding agent an implementation engine operating against a well-defined engineering specification, rather than asking it to simultaneously act as product manager, architect, database designer, security engineer, and programmer.
