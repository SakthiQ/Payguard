-- PayGuard Flyway Migration V2 — Performance Indexes
-- These indexes cover the hot-path queries identified during Phase 10 hardening.
-- Running without them causes full table scans on the most frequent operations.

-- TRANSACTIONS TABLE
-- Covers: sender transaction history, fraud velocity count query (countRecentTransfersBySender)
CREATE INDEX idx_transactions_sender_wallet_id ON transactions (sender_wallet_id);

-- Covers: receiver transaction history lookups
CREATE INDEX idx_transactions_receiver_wallet_id ON transactions (receiver_wallet_id);

-- Covers: fraud review queries filtering by status (FLAGGED, COMPLETED, DECLINED)
CREATE INDEX idx_transactions_status ON transactions (status);

-- Covers: fraud velocity rule time-window query (WHERE created_at >= ?)
CREATE INDEX idx_transactions_created_at ON transactions (created_at);

-- Composite index: covers the full velocity rule query pattern efficiently
-- countRecentTransfersBySender(walletId, since) → sender_wallet_id + status + created_at
CREATE INDEX idx_transactions_sender_status_created ON transactions (sender_wallet_id, status, created_at);

-- FRAUD_FLAGS TABLE
-- Covers: getPendingFlags() → WHERE status = 'UNDER_REVIEW'
CREATE INDEX idx_fraud_flags_status ON fraud_flags (status);

-- Covers: join from fraud_flags to transactions
CREATE INDEX idx_fraud_flags_transaction_id ON fraud_flags (transaction_id);

-- IDEMPOTENCY_KEYS TABLE
-- Covers: lookup by user (audit queries, cleanup jobs)
CREATE INDEX idx_idempotency_keys_user_id ON idempotency_keys (user_id);
