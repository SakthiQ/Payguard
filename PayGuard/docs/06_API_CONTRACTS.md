# PayGuard — API Contracts Specification

## 1. Authentication Endpoints

### 1.1 Register User
* **Method**: `POST`
* **Path**: `/api/v1/auth/register`
* **Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "first_name": "Alice",
  "last_name": "Smith"
}
```
* **Response `201 Created`**:
```json
{
  "user_id": 1,
  "email": "user@example.com",
  "role": "ROLE_CUSTOMER",
  "wallet_number": "ACC-9182374615",
  "message": "User registered successfully."
}
```

### 1.2 Login User
* **Method**: `POST`
* **Path**: `/api/v1/auth/login`
* **Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```
* **Response Header**:
```http
Set-Cookie: jwt=eyJhbGciOiJIUzI1NiJ9...; Path=/; Secure; HttpOnly; SameSite=Strict
```
* **Response `200 OK`**:
```json
{
  "user_id": 1,
  "email": "user@example.com",
  "role": "ROLE_CUSTOMER"
}
```

---

## 2. Wallet Endpoints

### 2.1 Get Wallet Details
* **Method**: `GET`
* **Path**: `/api/v1/wallets/{id}`
* **Response `200 OK`**:
```json
{
  "wallet_id": 1,
  "account_number": "ACC-9182374615",
  "balance": "1250.5000",
  "currency": "USD",
  "status": "ACTIVE"
}
```

### 2.2 Sandbox Wallet Deposit
* **Method**: `POST`
* **Path**: `/api/v1/wallets/{id}/deposit`
* **Request Body**:
```json
{
  "amount": "500.0000"
}
```
* **Response `200 OK`**:
```json
{
  "wallet_id": 1,
  "new_balance": "1750.5000",
  "transaction_reference": "DEP-771239"
}
```

---

## 3. Transaction Endpoints

### 3.1 Initiate Transfer
* **Method**: `POST`
* **Path**: `/api/v1/transactions`
* **Headers**: `Idempotency-Key: 7f2a9d8c-4b3e-4d1a-8c9f-1b2c3d4e5f6a`
* **Request Body**:
```json
{
  "recipient_account_number": "ACC-5519283741",
  "amount": "250.0000",
  "description": "Lunch payment"
}
```
* **Response `200 OK` (Processed Immediately)**:
```json
{
  "transaction_reference": "TXN-991203-A",
  "sender_account_number": "ACC-9182374615",
  "recipient_account_number": "ACC-5519283741",
  "amount": "250.0000",
  "status": "COMPLETED",
  "created_at": "2026-08-20T22:45:00Z"
}
```
* **Response `202 Accepted` (Flagged for Analyst Review)**:
```json
{
  "transaction_reference": "TXN-991203-B",
  "status": "FLAGGED",
  "message": "Transaction flagged for potential risk evaluation."
}
```
* **Response `409 Conflict` (Duplicate Key Currently Processing)**:
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "A transaction with this Idempotency-Key is currently in progress."
}
```

---

## 4. Fraud Analyst Endpoints

### 4.1 Get Flagged Queue
* **Method**: `GET`
* **Path**: `/api/v1/fraud/flags`
* **Response `200 OK`**:
```json
[
  {
    "flag_id": 10,
    "transaction_reference": "TXN-991203-B",
    "rule_code": "HIGH_AMOUNT",
    "risk_score": 85,
    "reason": "Transfer amount $6,000.00 exceeds threshold $5,000.00",
    "status": "UNDER_REVIEW"
  }
]
```

### 4.2 Review Fraud Flag
* **Method**: `PUT`
* **Path**: `/api/v1/fraud/flags/{id}/review`
* **Request Body**:
```json
{
  "action": "APPROVE", // APPROVE or DECLINE
  "notes": "Verified customer via phone call."
}
```
* **Response `200 OK`**:
```json
{
  "flag_id": 10,
  "status": "APPROVED",
  "transaction_status": "COMPLETED"
}
```
