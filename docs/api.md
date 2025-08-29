# API Documentation

## Overview

The Wallet Service provides a RESTful API for managing digital wallets and transactions. All endpoints are secured and require authentication.

## Base URL

```
https://api.wallet-service.com/v1
```

## Authentication

All requests must include a valid JWT token in the Authorization header:

```
Authorization: Bearer <jwt_token>
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
  "userId": "string",
  "currency": "string"
}
```

#### Response
```json
{
  "walletId": "string",
  "userId": "string",
  "currency": "string",
  "balance": "0.00",
  "status": "ACTIVE",
  "createdAt": "2024-03-21T10:30:00Z"
}
```

### Get Balance

Retrieves the current balance of a wallet.

```http
GET /wallets/{walletId}/balance
```

#### Response
```json
{
  "walletId": "string",
  "balance": "100.50",
  "currency": "USD",
  "timestamp": "2024-03-21T10:30:00Z"
}
```

### Get Historical Balance

Retrieves the balance at a specific point in time.

```http
GET /wallets/{walletId}/balance/historical
```

#### Query Parameters
```
timestamp: ISO-8601 formatted datetime
```

#### Response
```json
{
  "walletId": "string",
  "balance": "95.20",
  "currency": "USD",
  "timestamp": "2024-03-20T15:45:00Z"
}
```

### Deposit Funds

Adds funds to a wallet.

```http
POST /wallets/{walletId}/deposit
```

#### Request Body
```json
{
  "amount": "50.00",
  "referenceId": "string",
  "description": "string"
}
```

#### Response
```json
{
  "transactionId": "string",
  "walletId": "string",
  "type": "DEPOSIT",
  "amount": "50.00",
  "status": "COMPLETED",
  "timestamp": "2024-03-21T10:30:00Z"
}
```

### Withdraw Funds

Removes funds from a wallet.

```http
POST /wallets/{walletId}/withdraw
```

#### Request Body
```json
{
  "amount": "25.00",
  "referenceId": "string",
  "description": "string"
}
```

#### Response
```json
{
  "transactionId": "string",
  "walletId": "string",
  "type": "WITHDRAWAL",
  "amount": "25.00",
  "status": "COMPLETED",
  "timestamp": "2024-03-21T10:30:00Z"
}
```

### Transfer Funds

Transfers funds between wallets.

```http
POST /wallets/{sourceWalletId}/transfer
```

#### Request Body
```json
{
  "destinationWalletId": "string",
  "amount": "30.00",
  "referenceId": "string",
  "description": "string"
}
```

#### Response
```json
{
  "transactionId": "string",
  "sourceWalletId": "string",
  "destinationWalletId": "string",
  "type": "TRANSFER",
  "amount": "30.00",
  "status": "COMPLETED",
  "timestamp": "2024-03-21T10:30:00Z"
}
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
- `UNAUTHORIZED`: Invalid or missing authentication
- `FORBIDDEN`: Insufficient permissions
- `VALIDATION_ERROR`: Invalid request parameters

## Rate Limiting

- Default rate limit: 100 requests per minute per API key
- Deposit/Withdraw/Transfer: 20 requests per minute per wallet
- Balance queries: 1000 requests per minute per wallet

## Webhook Notifications

Subscribe to transaction events via webhooks:

```http
POST /webhooks
```

#### Request Body
```json
{
  "url": "string",
  "events": ["DEPOSIT", "WITHDRAWAL", "TRANSFER"],
  "secret": "string"
}
```

## SDK Support

Official SDKs are available for:
- Java
- Python
- Node.js
- Go
