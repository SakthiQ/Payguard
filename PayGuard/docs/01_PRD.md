# PayGuard — Product Requirements Document (PRD)

## 1. Product Summary
**PayGuard** is a production-inspired fintech backend platform simulating an enterprise wallet system. It enforces idempotent money transfers, role-based authorization, deterministic real-time fraud scoring, database-level concurrency management, and asynchronous immutable audit logging.

---

## 2. Target Roles & Responsibilities

### 2.1 Customer
* **Registration & Authentication**: Register a personal account, log in securely, receive HTTP-Only JWT cookies, and manage session credentials.
* **Wallet Management**: Automatically provisioned wallet upon registration; view balance, wallet account number, and wallet status.
* **Sandbox Wallet Top-Up**: Fund wallet balance with simulated deposits for test environments (`POST /api/v1/wallets/{id}/deposit`).
* **Beneficiary Management**: Add, list, and delete saved recipient wallets.
* **Money Transfers**: Initiate Peer-to-Peer (P2P) transfers to other wallets using strict idempotency keys (`Idempotency-Key` header).
* **Transaction History**: View real-time status and historical log of initiated/received payments.

### 2.2 Fraud Analyst
* **Fraud Flag Queue**: Inspect flagged payments requiring manual review.
* **Risk Inspection**: View triggered fraud rules (e.g., Velocity trigger, High Amount threshold), risk score, sender/receiver details, and amount.
* **Decision Execution**: Approve or Decline suspicious transactions with mandatory investigation notes.

### 2.3 System Administrator
* **User & Wallet Administration**: Search users, freeze/unfreeze compromised accounts.
* **Fraud Rule Configuration**: View and update threshold values for velocity and single-transfer limits.
* **Operational Monitoring**: View system-wide metrics (total processed volume, flag rate, average processing latency).

---

## 3. Core Feature Requirements & User Stories

### US-1: Idempotent Peer-to-Peer Transfer
* **As a** Customer  
* **I want to** transfer money to another wallet safely  
* **So that** network retries or duplicate button clicks never cause double debits.  
* **Acceptance Criteria**:
  1. Transfer request must carry an `Idempotency-Key` HTTP header.
  2. Concurrent requests with the same key return `409 Conflict` while in progress.
  3. Subsequent retries after completion return the exact cached HTTP response payload without re-debiting.
  4. Balance calculations use `BigDecimal` / `DECIMAL(19,4)`.

### US-2: Deterministic Fraud Scoring Engine
* **As a** Fraud Analyst / Risk System  
* **I want to** evaluate every transfer before processing  
* **So that** high-risk or rapid-fire transactions are intercepted immediately.  
* **Acceptance Criteria**:
  1. Rule 1 (High Amount): Flag transfers > $5,000.00.
  2. Rule 2 (Velocity): Flag when sender attempts > 3 transfers within a 5-minute rolling window.
  3. Rule 3 (Blacklist): Immediate rejection if sender or receiver account status is `FROZEN` or blacklisted.
  4. Flagged transfers transition to `FLAGGED` state and enter the Analyst Queue without debiting the sender.

### US-3: Sandbox Wallet Deposit
* **As a** Customer  
* **I want to** top up my simulated wallet  
* **So that** I can test money transfers without raw database manipulations.  
* **Acceptance Criteria**:
  1. Endpoint `POST /api/v1/wallets/{id}/deposit` increases balance by specified positive amount.
  2. Produces a `WALLET_TOPUP` transaction record and asynchronous audit event.

---

## 4. Key Workflows & State Diagrams

### 4.1 Payment Transfer Lifecycle
```text
[Customer Request] ──> [JWT Cookie Valid?] ──> [Idempotency Key Check]
                                                      │
                                                      ├──> (Duplicate) ──> Return Cached Response
                                                      v (New Key)
                                          [Validate Balance & State]
                                                      │
                                                      v
                                            [Run Fraud Rules]
                                            /               \
                                    (Passed)                (Triggered)
                                       │                         │
                                       v                         v
                           [Lock Wallets (min(ID))]     [State: FLAGGED]
                                       │                         │
                              [Debit & Credit]             [Fraud Queue]
                                       │                         │
                                       v                     [Analyst Review]
                               [State: COMPLETED]          /              \
                                       │               (Approve)       (Decline)
                                       v                   │               │
                            [Async Audit Log] ───> [Process Transfer] [State: DECLINED]
```

---

## 5. Non-Functional Requirements (NFRs)
* **Data Consistency**: Zero balance drift; strict ACID transactional boundary across debit/credit steps.
* **Deadlock Prevention**: Deterministic lock ordering on wallet database rows sorted by ID.
* **Security**: Password hashing via BCrypt (cost factor 12); JWT access tokens stored in HTTP-Only, Secure, SameSite cookies; zero API exposure of internal JPA entities.
* **Auditability**: 100% of state-changing operations logged asynchronously to MongoDB.
