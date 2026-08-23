# PayGuard — Low-Level Design (LLD)

## 1. Package Structure Strategy

The codebase adheres to **Package-by-Feature** organization:

```text
com.payguard
│
├── auth
│   ├── controller      # AuthController (login, register, logout)
│   ├── dto             # LoginRequest, RegisterRequest, AuthResponse
│   └── service         # AuthService, JwtTokenProvider
│
├── user
│   ├── entity          # User, Role (CUSTOMER, FRAUD_ANALYST, ADMIN)
│   ├── repository      # UserRepository
│   └── service         # UserService
│
├── wallet
│   ├── controller      # WalletController (get, balance, deposit)
│   ├── dto             # WalletResponse, DepositRequest
│   ├── entity          # Wallet, WalletStatus (ACTIVE, FROZEN)
│   ├── repository      # WalletRepository
│   └── service         # WalletService
│
├── transaction
│   ├── controller      # TransactionController (transfer, history)
│   ├── dto             # TransferRequest, TransferResponse, TransactionResponse
│   ├── entity          # Transaction, TransactionStatus, TransactionType
│   ├── repository      # TransactionRepository, IdempotencyKeyRepository
│   └── service         # TransferService, IdempotencyService
│
├── fraud
│   ├── controller      # FraudController (flags queue, approve, decline)
│   ├── dto             # FraudFlagResponse, FraudReviewRequest
│   ├── entity          # FraudFlag, FraudRule, FlagStatus
│   ├── repository      # FraudFlagRepository, FraudRuleRepository
│   └── service         # FraudEngineService, FraudReviewService
│
├── audit
│   ├── document        # AuditLogDocument (MongoDB Document)
│   ├── event           # DomainAuditEvent (Spring ApplicationEvent)
│   ├── repository      # AuditLogMongoRepository
│   └── listener       # AuditEventListener (@TransactionalEventListener)
│
├── security
│   ├── config          # SecurityConfig, JwtCookieProperties
│   ├── filter          # JwtAuthenticationFilter
│   └── userdetails     # CustomUserDetailsService
│
├── common
│   ├── exception       # GlobalExceptionHandler, Custom Exceptions
│   └── util            # LockUtils, MonetaryUtils
```

---

## 2. Enums & State Machines

### 2.1 TransactionStatus State Machine
```text
  CREATED ──> VALIDATING ──┬──> COMPLETED
                           ├──> FLAGGED ──> UNDER_REVIEW ──┬──> APPROVED ──> COMPLETED
                           │                               └──> DECLINED
                           └──> FAILED
```

### 2.2 IdempotencyState State Machine
```text
  IN_PROGRESS ──┬──> COMPLETED (stores response payload & status 200)
                └──> FAILED (deletes/resets key for safe retry)
```

---

## 3. Key Class & Method Specifications

### 3.1 `TransferService.java`
* `TransferResponse processTransfer(TransferRequest request, String idempotencyKeyHeader, User principal)`
  1. Calls `idempotencyService.acquireLock(idempotencyKeyHeader)`.
  2. Executes fraud scoring via `fraudEngineService.evaluate(request)`.
  3. If risk flagged: saves `Transaction` (`FLAGGED`), creates `FraudFlag`, emits `TRANSFER_FLAGGED` audit event, returns 202 Accepted.
  4. If low risk: sorts wallet IDs (`min(id)` then `max(id)`), calls `walletRepository.findWithPessimisticWriteLock(id)` for both wallets.
  5. Validates balance >= amount. Debits sender, credits receiver.
  6. Saves `Transaction` (`COMPLETED`). Emits `TRANSFER_COMPLETED` audit event.
  7. Updates idempotency state to `COMPLETED` with serialized response.

### 3.2 `AuditEventListener.java`
* `@TransactionalEventListener(phase = AFTER_COMMIT)`
* `public void handleDomainAuditEvent(DomainAuditEvent event)`
  - Converts domain event to `AuditLogDocument`.
  - Persists document to MongoDB `audit_logs` collection.
  - Surrounds write with try-catch to log error alerts without throwing exceptions upstream.
