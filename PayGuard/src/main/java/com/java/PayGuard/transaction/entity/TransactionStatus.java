package com.java.PayGuard.transaction.entity;

public enum TransactionStatus {
    CREATED,
    VALIDATING,
    FLAGGED,
    COMPLETED,
    DECLINED,
    FAILED
}
