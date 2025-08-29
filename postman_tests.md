# Wallet Service API Tests

## Environment Variables
```
BASE_URL=http://localhost:8080
```

## Authentication
First, let's create a JWT token for testing (this is a development token, not for production use):

```bash
# Generate JWT token
TOKEN=$(curl -X POST "${BASE_URL}/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"userId": "test-user-1"}')
```

## Wallet Operations

### Create Wallet
```bash
# Create wallet for test-user-1
curl -X POST "${BASE_URL}/api/v1/wallets" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-1",
    "currency": "EUR"
  }'

# Create wallet for test-user-2
curl -X POST "${BASE_URL}/api/v1/wallets" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-2",
    "currency": "EUR"
  }'

# Try to create duplicate wallet (should fail)
curl -X POST "${BASE_URL}/api/v1/wallets" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-1",
    "currency": "EUR"
  }'
```

### Get Wallet
```bash
# Get existing wallet
curl -X GET "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer ${TOKEN}"

# Get non-existent wallet (should fail)
curl -X GET "${BASE_URL}/api/v1/wallets/non-existent-id" \
  -H "Authorization: Bearer ${TOKEN}"

# Get wallet without authorization (should fail)
curl -X GET "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000"
```

### Get User Wallets
```bash
# Get all wallets for user
curl -X GET "${BASE_URL}/api/v1/wallets?userId=test-user-1" \
  -H "Authorization: Bearer ${TOKEN}"

# Get wallets with invalid user ID
curl -X GET "${BASE_URL}/api/v1/wallets?userId=non-existent-user" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Deposit Funds
```bash
# Deposit valid amount
curl -X POST "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/deposit" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.00,
    "referenceId": "DEP-TEST-001",
    "description": "Test deposit"
  }'

# Deposit with duplicate reference ID (should fail)
curl -X POST "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/deposit" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.00,
    "referenceId": "DEP-TEST-001",
    "description": "Test deposit"
  }'

# Deposit negative amount (should fail)
curl -X POST "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/deposit" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": -100.00,
    "referenceId": "DEP-TEST-002",
    "description": "Test deposit"
  }'
```

### Withdraw Funds
```bash
# Withdraw valid amount
curl -X POST "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/withdraw" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50.00,
    "referenceId": "WD-TEST-001",
    "description": "Test withdrawal"
  }'

# Withdraw more than balance (should fail)
curl -X POST "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/withdraw" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100000.00,
    "referenceId": "WD-TEST-002",
    "description": "Test withdrawal"
  }'

# Withdraw negative amount (should fail)
curl -X POST "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/withdraw" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": -50.00,
    "referenceId": "WD-TEST-003",
    "description": "Test withdrawal"
  }'
```

### Transfer Funds
```bash
# Transfer valid amount
curl -X POST "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/transfer" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 25.00,
    "referenceId": "TRF-TEST-001",
    "description": "Test transfer",
    "destinationWalletId": "550e8400-e29b-41d4-a716-446655440001"
  }'

# Transfer to non-existent wallet (should fail)
curl -X POST "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/transfer" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 25.00,
    "referenceId": "TRF-TEST-002",
    "description": "Test transfer",
    "destinationWalletId": "non-existent-id"
  }'

# Transfer more than balance (should fail)
curl -X POST "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/transfer" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100000.00,
    "referenceId": "TRF-TEST-003",
    "description": "Test transfer",
    "destinationWalletId": "550e8400-e29b-41d4-a716-446655440001"
  }'
```

### Get Transaction History
```bash
# Get all transactions for wallet
curl -X GET "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/transactions" \
  -H "Authorization: Bearer ${TOKEN}"

# Get transactions with date range
curl -X GET "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/transactions?fromDate=2024-01-01T00:00:00Z&toDate=2024-12-31T23:59:59Z" \
  -H "Authorization: Bearer ${TOKEN}"

# Get transactions with invalid date range (should fail)
curl -X GET "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/transactions?fromDate=2024-12-31T23:59:59Z&toDate=2024-01-01T00:00:00Z" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Get Historical Balance
```bash
# Get balance at specific point in time
curl -X GET "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/balance/historical?timestamp=2024-01-01T00:00:00Z" \
  -H "Authorization: Bearer ${TOKEN}"

# Get balance with invalid timestamp (should fail)
curl -X GET "${BASE_URL}/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/balance/historical?timestamp=invalid-date" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Health Check
```bash
# Get application health status
curl -X GET "${BASE_URL}/q/health"

# Get application health status (live)
curl -X GET "${BASE_URL}/q/health/live"

# Get application health status (ready)
curl -X GET "${BASE_URL}/q/health/ready"
```

### Metrics
```bash
# Get application metrics
curl -X GET "${BASE_URL}/metrics"

# Get application metrics (Prometheus format)
curl -X GET "${BASE_URL}/metrics" \
  -H "Accept: application/openmetrics-text"
```

## OpenAPI Documentation
```bash
# Get OpenAPI specification
curl -X GET "${BASE_URL}/q/openapi"

# Get OpenAPI specification (YAML format)
curl -X GET "${BASE_URL}/q/openapi?format=yaml"
```

## Import Instructions

1. Open Postman
2. Click on "Import" button
3. Select "Raw text" tab
4. Copy each curl command and paste it into Postman
5. Click "Continue" and then "Import"
6. Create an environment with the variable `BASE_URL` set to `http://localhost:8080`
7. Create a variable `TOKEN` in your environment to store the JWT token

## Test Sequence

For proper testing, follow this sequence:

1. Generate JWT token
2. Create wallets
3. Test wallet retrieval
4. Test deposits
5. Test withdrawals
6. Test transfers
7. Test transaction history
8. Test historical balance
9. Test health and metrics endpoints

## Notes

- Replace `${TOKEN}` with your actual JWT token
- The test data uses UUIDs that match our database seed data
- All amounts are in the wallet's currency
- Reference IDs must be unique across all transactions
- Timestamps should be in ISO-8601 format
- Some tests are designed to fail to verify error handling
