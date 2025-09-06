# API Documentation

## Overview

The Wallet Service provides a RESTful API for managing digital wallets and transactions. Supports multiple currencies with USD as the primary currency for examples.

## Base URL

**Local Development:**
```
http://localhost:8080/api/v1
```

**Production:**
```
https://api.wallet-service.com/v1
```



## Endpoints

### Create Wallet

Creates a new wallet for a user.

```http
POST /wallets
```

#### Request Body
```json
{
  "userId": "user123",
  "currency": "USD"
}
```

#### Response
```json
{
  "walletId": "wallet-abc123",
  "userId": "user123", 
  "currency": "USD",
  "balance": 0.00
}
```

#### Example
```bash
curl -X POST http://localhost:8080/api/v1/wallets \
  -H "Content-Type: application/json" \
  -d '{"userId": "user123", "currency": "USD"}'
```

### Get Balance

Retrieves the current balance of a wallet.

```http
GET /wallets/{walletId}/balance
```

#### Response
```json
{
  "walletId": "wallet-abc123",
  "balance": 100.00,
  "currency": "USD"
}
```

#### Example
```bash
curl http://localhost:8080/api/v1/wallets/wallet-abc123/balance
```

### Get Historical Balance

Retrieves the balance at a specific point in time.

```http
GET /wallets/{walletId}/balance/history?timestamp={timestamp}
```

#### Query Parameters
```
timestamp: ISO-8601 formatted datetime (e.g., 2024-01-15T10:30:00Z)
```

#### Response
```json
{
  "walletId": "wallet-abc123",
  "balance": 45.00,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### Example
```bash
curl "http://localhost:8080/api/v1/wallets/wallet-abc123/balance/history?timestamp=2024-01-15T10:30:00Z"
```

### Deposit Funds

Adds funds to a wallet.

```http
POST /wallets/{walletId}/deposit
```

#### Request Body
```json
{
  "amount": 100.00,
  "referenceId": "deposit-001"
}
```

#### Response
```json
{
  "walletId": "wallet-abc123",
  "newBalance": 100.00,
  "transactionId": "txn-xyz789"
}
```

#### Example
```bash
curl -X POST http://localhost:8080/api/v1/wallets/wallet-abc123/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00, "referenceId": "deposit-001"}'
```

### Withdraw Funds

Removes funds from a wallet.

```http
POST /wallets/{walletId}/withdraw
```

#### Request Body
```json
{
  "amount": 25.00,
  "referenceId": "withdraw-001"
}
```

#### Response
```json
{
  "walletId": "wallet-abc123",
  "newBalance": 75.00,
  "transactionId": "txn-def456"
}
```

#### Example
```bash
curl -X POST http://localhost:8080/api/v1/wallets/wallet-abc123/withdraw \
  -H "Content-Type: application/json" \
  -d '{"amount": 25.00, "referenceId": "withdraw-001"}'
```

### Transfer Funds

Transfers funds between wallets.

```http
POST /wallets/{sourceWalletId}/transfer
```

#### Request Body
```json
{
  "toWalletId": "wallet-def456",
  "amount": 30.00,
  "referenceId": "transfer-001"
}
```

#### Response
```json
{
  "fromWalletId": "wallet-abc123",
  "toWalletId": "wallet-def456",
  "amount": 30.00,
  "transactionId": "txn-ghi789"
}
```

#### Example
```bash
curl -X POST http://localhost:8080/api/v1/wallets/wallet-abc123/transfer \
  -H "Content-Type: application/json" \
  -d '{"toWalletId": "wallet-def456", "amount": 30.00, "referenceId": "transfer-001"}'
```

### List User's Wallets

Retrieves all wallets for a specific user.

```http
GET /users/{userId}/wallets
```

#### Response
```json
[
  {
    "walletId": "wallet-abc123",
    "currency": "USD",
    "balance": 45.00
  }
]
```

#### Example
```bash
curl http://localhost:8080/api/v1/users/user123/wallets
```

## Error Responses

All error responses follow this format:

```json
{
  "error": {
    "code": "string",
    "message": "string",
    "details": {}
  },
  "requestId": "string",
  "timestamp": "2024-03-21T10:30:00Z"
}
```

### Common Error Codes

- `INSUFFICIENT_FUNDS`: Wallet has insufficient balance
- `WALLET_NOT_FOUND`: Specified wallet doesn't exist
- `INVALID_AMOUNT`: Amount is invalid or negative
- `DUPLICATE_REFERENCE`: Reference ID already used

- `VALIDATION_ERROR`: Invalid request parameters

