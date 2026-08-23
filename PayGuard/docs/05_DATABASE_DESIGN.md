# PayGuard — Database Design Specification

## 1. Relational Schema (MySQL 8.0)

Managed exclusively through Flyway migrations (`V1__init_schema.sql`).

```sql
-- 1. USERS TABLE
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_CUSTOMER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. WALLETS TABLE
CREATE TABLE wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    account_number VARCHAR(34) NOT NULL UNIQUE,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. BENEFICIARIES TABLE
CREATE TABLE beneficiaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    beneficiary_wallet_number VARCHAR(34) NOT NULL,
    nick_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_beneficiary_owner FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_beneficiary UNIQUE (owner_user_id, beneficiary_wallet_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. IDEMPOTENCY_KEYS TABLE
CREATE TABLE idempotency_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL, -- IN_PROGRESS, COMPLETED, FAILED
    response_code INT NULL,
    response_body TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. TRANSACTIONS TABLE
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_reference VARCHAR(64) NOT NULL UNIQUE,
    sender_wallet_id BIGINT NOT NULL,
    receiver_wallet_id BIGINT NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    type VARCHAR(30) NOT NULL, -- TRANSFER, DEPOSIT
    status VARCHAR(30) NOT NULL, -- CREATED, VALIDATING, FLAGGED, COMPLETED, DECLINED, FAILED
    idempotency_key_id BIGINT NULL,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tx_sender FOREIGN KEY (sender_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_tx_receiver FOREIGN KEY (receiver_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_tx_idempotency FOREIGN KEY (idempotency_key_id) REFERENCES idempotency_keys(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. FRAUD_RULES TABLE
CREATE TABLE fraud_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    rule_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    threshold_value DECIMAL(19,4) NOT NULL,
    time_window_seconds INT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. FRAUD_FLAGS TABLE
CREATE TABLE fraud_flags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    triggered_rule_code VARCHAR(50) NOT NULL,
    risk_score INT NOT NULL,
    flag_reason VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UNDER_REVIEW', -- UNDER_REVIEW, APPROVED, DECLINED
    reviewer_user_id BIGINT NULL,
    review_notes TEXT NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ff_tx FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_ff_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 2. Document Schema (MongoDB 7.0)

Collection: `audit_logs`

```json
{
  "_id": "ObjectId('65d4b8e21a3b4c0012345678')",
  "event_id": "evt_9a8b7c6d5e",
  "event_type": "TRANSFER_COMPLETED",
  "actor_user_id": 42,
  "actor_email": "user@example.com",
  "actor_role": "ROLE_CUSTOMER",
  "resource_type": "TRANSACTION",
  "resource_id": "1001",
  "payload": {
    "transaction_reference": "TXN-8823-9912",
    "sender_wallet_id": 10,
    "receiver_wallet_id": 14,
    "amount": "150.0000",
    "currency": "USD",
    "status": "COMPLETED"
  },
  "ip_address": "192.168.1.10",
  "timestamp": "2026-08-20T22:45:00.000Z"
}
```
