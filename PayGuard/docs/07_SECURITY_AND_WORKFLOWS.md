# PayGuard — Security & Engineering Protocols

## 1. Security Architecture

### 1.1 HTTP-Only Cookie Authentication Pipeline
```text
[HTTP Request + Cookie: jwt=...] 
       │
       v
[JwtAuthenticationFilter]
       │
       ├── Reads "jwt" Cookie value
       ├── Parses signature using HMAC-SHA256 Secret Key
       ├── Verifies Expiration (max 3600s)
       ├── Extracts Email & Authorities (`ROLE_CUSTOMER`, `ROLE_FRAUD_ANALYST`, `ROLE_ADMIN`)
       v
[SecurityContextHolder] ──> Enables @PreAuthorize on Controllers
```

### 1.2 RBAC Matrix
| Endpoint Group | Allowed Roles |
| :--- | :--- |
| `/api/v1/auth/**` | Anonymous / Public |
| `/api/v1/wallets/me` | `ROLE_CUSTOMER`, `ROLE_ADMIN` |
| `/api/v1/wallets/{id}/deposit` | `ROLE_CUSTOMER`, `ROLE_ADMIN` |
| `/api/v1/transactions` | `ROLE_CUSTOMER` |
| `/api/v1/fraud/**` | `ROLE_FRAUD_ANALYST`, `ROLE_ADMIN` |
| `/api/v1/admin/**` | `ROLE_ADMIN` |

---

## 2. Concurrency & Deadlock Prevention Protocol

### 2.1 The Circular Wait Problem
If Customer A transfers to Customer B at the exact same instant Customer B transfers to Customer A:
* Thread 1 locks Wallet A, attempts to lock Wallet B.
* Thread 2 locks Wallet B, attempts to lock Wallet A.
* Result: **Database Deadlock Exception (`DeadlockLoserDataAccessException`)**.

### 2.2 Deterministic Lock Ordering Solution
PayGuard enforces numerical wallet ID ordering before requesting locks:

```java
public class LockUtils {
    public static OrderedLocks sort(Long senderId, Long receiverId) {
        if (senderId.compareTo(receiverId) < 0) {
            return new OrderedLocks(senderId, receiverId);
        } else {
            return new OrderedLocks(receiverId, senderId);
        }
    }
}
```

Both Thread 1 and Thread 2 lock the wallet with the smaller ID first, forcing Thread 2 to queue cleanly behind Thread 1. Deadlock probability is reduced to zero.

---

## 3. Idempotency Key Lifecycle Engine

### 3.1 Idempotency Table Schema
`idempotency_keys` table holds `idempotency_key` (VARCHAR UNIQUE), `status` (`IN_PROGRESS`, `COMPLETED`, `FAILED`), `response_code`, and `response_body`.

### 3.2 Key Protocol Execution
```java
@Transactional
public void processIdempotencyKey(String keyHeader, Long userId) {
    try {
        IdempotencyKey key = new IdempotencyKey(keyHeader, userId, "IN_PROGRESS");
        idempotencyRepository.saveAndFlush(key);
    } catch (DataIntegrityViolationException ex) {
        IdempotencyKey existing = idempotencyRepository.findByKey(keyHeader);
        if ("IN_PROGRESS".equals(existing.getStatus())) {
            throw new DuplicateTransactionException("Transaction with key is currently processing.", HttpStatus.CONFLICT);
        } else if ("COMPLETED".equals(existing.getStatus())) {
            throw new IdempotentResponseException(existing.getResponseCode(), existing.getResponseBody());
        }
    }
}
```

---

## 4. Asynchronous Dual-DB Audit Protocol

### 4.1 Decoupling Financial Transactions from Audit Writes
Audit operations in MongoDB are executed asynchronously to isolate MySQL transaction commits:

```java
@Component
public class AuditEventListener {

    private final AuditLogMongoRepository auditLogRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainAuditEvent(DomainAuditEvent event) {
        try {
            AuditLogDocument doc = AuditLogDocument.fromEvent(event);
            auditLogRepository.save(doc);
        } catch (Exception ex) {
            log.error("Failed to persist MongoDB audit event for eventId: {}", event.getEventId(), ex);
            // Alerting integration can trigger here without rolling back MySQL transaction
        }
    }
}
```
