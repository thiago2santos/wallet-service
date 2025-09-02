# Authentication & Authorization Demo

This document demonstrates how to use the newly implemented JWT authentication and role-based access control (RBAC) system in the Wallet Service.

## Overview

The Wallet Service now implements comprehensive security features:

- **JWT Authentication** with RS256 algorithm
- **Role-Based Access Control (RBAC)** with user and admin roles
- **Protected API endpoints** requiring authentication
- **Permission-based authorization** following the security documentation

## Roles and Permissions

### User Role (`user`)
- `wallet:read` - View wallet information and balances
- `wallet:deposit` - Deposit funds to wallets
- `wallet:withdraw` - Withdraw funds from wallets
- `wallet:transfer` - Transfer funds between wallets

### Admin Role (`admin`)
- All user permissions plus:
- `wallet:write` - Create and modify wallets
- `wallet:freeze` - Freeze wallet operations
- `wallet:close` - Close wallets
- `system:metrics` - Access system metrics and monitoring

## Getting Started

### 1. Start the Application

```bash
# Start the application with all dependencies
docker-compose up -d
mvn quarkus:dev
```

### 2. Obtain Authentication Tokens

The service provides a demo authentication endpoint for testing:

#### Login as User
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user",
    "password": "user123"
  }'
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "user": "user"
}
```

#### Login as Admin
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

### 3. Use the API with Authentication

#### Create a Wallet (User or Admin)
```bash
# Save the token from login response
USER_TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X POST http://localhost:8080/api/v1/wallets \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "currency": "USD"
  }'
```

#### Get Wallet Information (User or Admin)
```bash
curl -X GET http://localhost:8080/api/v1/wallets/{walletId} \
  -H "Authorization: Bearer $USER_TOKEN"
```

#### Deposit Funds (User or Admin)
```bash
curl -X POST http://localhost:8080/api/v1/wallets/{walletId}/deposit \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "100.00",
    "referenceId": "dep-123",
    "description": "Initial deposit"
  }'
```

#### Access Admin Metrics (Admin Only)
```bash
# This will work with admin token
ADMIN_TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X GET http://localhost:8080/api/v1/admin/metrics \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# This will fail with user token (403 Forbidden)
curl -X GET http://localhost:8080/api/v1/admin/metrics \
  -H "Authorization: Bearer $USER_TOKEN"
```

## Testing Authentication Scenarios

### 1. Unauthenticated Request (Should Fail)
```bash
curl -X POST http://localhost:8080/api/v1/wallets \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "currency": "USD"
  }'

# Expected: 401 Unauthorized
```

### 2. Invalid Token (Should Fail)
```bash
curl -X POST http://localhost:8080/api/v1/wallets \
  -H "Authorization: Bearer invalid-token" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "currency": "USD"
  }'

# Expected: 401 Unauthorized
```

### 3. User Accessing Admin Endpoint (Should Fail)
```bash
curl -X GET http://localhost:8080/api/v1/admin/metrics \
  -H "Authorization: Bearer $USER_TOKEN"

# Expected: 403 Forbidden
```

### 4. Expired Token (Should Fail)
Tokens expire after 1 hour. After expiration:
```bash
curl -X GET http://localhost:8080/api/v1/wallets/{walletId} \
  -H "Authorization: Bearer $EXPIRED_TOKEN"

# Expected: 401 Unauthorized
```

## Complete Workflow Example

Here's a complete example showing the authentication workflow:

```bash
#!/bin/bash

# 1. Login as user
echo "=== Logging in as user ==="
USER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "user123"}')

USER_TOKEN=$(echo $USER_RESPONSE | jq -r '.access_token')
echo "User token obtained: ${USER_TOKEN:0:50}..."

# 2. Create a wallet
echo -e "\n=== Creating wallet ==="
WALLET_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/wallets \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userId": "user123", "currency": "USD"}')

WALLET_ID=$(echo $WALLET_RESPONSE | grep -o '/api/v1/wallets/[^"]*' | sed 's|.*/||')
echo "Wallet created with ID: $WALLET_ID"

# 3. Deposit funds
echo -e "\n=== Depositing funds ==="
curl -s -X POST http://localhost:8080/api/v1/wallets/$WALLET_ID/deposit \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": "100.00", "referenceId": "dep-123", "description": "Test deposit"}'

# 4. Try to access admin endpoint (should fail)
echo -e "\n=== Trying to access admin endpoint (should fail) ==="
curl -s -X GET http://localhost:8080/api/v1/admin/metrics \
  -H "Authorization: Bearer $USER_TOKEN"

# 5. Login as admin
echo -e "\n\n=== Logging in as admin ==="
ADMIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}')

ADMIN_TOKEN=$(echo $ADMIN_RESPONSE | jq -r '.access_token')
echo "Admin token obtained: ${ADMIN_TOKEN:0:50}..."

# 6. Access admin endpoint (should work)
echo -e "\n=== Accessing admin endpoint ==="
curl -s -X GET http://localhost:8080/api/v1/admin/metrics \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .

echo -e "\n=== Demo completed ==="
```

## Security Features Implemented

### 1. JWT Token Validation
- RS256 algorithm for secure token signing
- Token expiration (1 hour)
- Issuer and audience validation
- Public key verification

### 2. Role-Based Access Control
- Method-level security annotations
- Role-based endpoint protection
- Permission-based authorization logic

### 3. Error Handling
- 401 Unauthorized for invalid/missing tokens
- 403 Forbidden for insufficient permissions
- Proper error response format

### 4. Security Headers
- Authorization header validation
- Bearer token format enforcement

## Production Considerations

⚠️ **Important**: This demo uses a simple authentication endpoint for testing. In production:

1. **Use External Identity Provider**: Integrate with Keycloak, Auth0, or similar
2. **Secure Key Management**: Use proper key rotation and management
3. **Token Refresh**: Implement refresh token mechanism
4. **Rate Limiting**: Add rate limiting to prevent abuse
5. **Audit Logging**: Log all authentication and authorization events
6. **HTTPS Only**: Ensure all communication is over HTTPS

## Testing

Run the security tests to verify the implementation:

```bash
# Run all security tests
mvn test -Dtest="*Security*"

# Run specific test classes
mvn test -Dtest=SecurityConfigTest
mvn test -Dtest=JwtTokenGeneratorTest
```

## Troubleshooting

### Common Issues

1. **401 Unauthorized**
   - Check if token is included in Authorization header
   - Verify token format: `Bearer <token>`
   - Ensure token hasn't expired

2. **403 Forbidden**
   - Verify user has required role for the endpoint
   - Check role assignments in JWT token

3. **Token Generation Fails**
   - Ensure private key file exists and is readable
   - Check JWT configuration in application.properties

### Debug Tips

1. **Enable Debug Logging**
   ```properties
   quarkus.log.category."io.quarkus.security".level=DEBUG
   quarkus.log.category."io.smallrye.jwt".level=DEBUG
   ```

2. **Inspect JWT Token**
   Use [jwt.io](https://jwt.io) to decode and inspect token contents

3. **Check Security Configuration**
   Verify application.properties has correct JWT settings
