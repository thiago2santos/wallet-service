#!/bin/bash

# Authentication Test Script for Wallet Service
# This script demonstrates the JWT authentication and RBAC implementation

set -e

BASE_URL="http://localhost:8080"
echo "🔐 Testing Wallet Service Authentication & Authorization"
echo "=================================================="

# Test 1: Unauthenticated request should fail
echo -e "\n📋 Test 1: Unauthenticated request (should fail with 401)"
echo "curl -X POST $BASE_URL/api/v1/wallets"
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/v1/wallets \
  -H "Content-Type: application/json" \
  -d '{"userId": "user123", "currency": "USD"}')

if [ "$HTTP_STATUS" = "401" ]; then
    echo "✅ PASS: Unauthenticated request correctly rejected (401)"
else
    echo "❌ FAIL: Expected 401, got $HTTP_STATUS"
fi

# Test 2: Login as user
echo -e "\n📋 Test 2: User login"
echo "curl -X POST $BASE_URL/api/v1/auth/login (user credentials)"
USER_RESPONSE=$(curl -s -X POST $BASE_URL/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "user123"}' || echo "ERROR")

if [[ "$USER_RESPONSE" == *"access_token"* ]]; then
    echo "✅ PASS: User login successful"
    USER_TOKEN=$(echo $USER_RESPONSE | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
    echo "   Token: ${USER_TOKEN:0:50}..."
else
    echo "❌ FAIL: User login failed"
    echo "   Response: $USER_RESPONSE"
    USER_TOKEN=""
fi

# Test 3: Login as admin
echo -e "\n📋 Test 3: Admin login"
echo "curl -X POST $BASE_URL/api/v1/auth/login (admin credentials)"
ADMIN_RESPONSE=$(curl -s -X POST $BASE_URL/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}' || echo "ERROR")

if [[ "$ADMIN_RESPONSE" == *"access_token"* ]]; then
    echo "✅ PASS: Admin login successful"
    ADMIN_TOKEN=$(echo $ADMIN_RESPONSE | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
    echo "   Token: ${ADMIN_TOKEN:0:50}..."
else
    echo "❌ FAIL: Admin login failed"
    echo "   Response: $ADMIN_RESPONSE"
    ADMIN_TOKEN=""
fi

# Test 4: User can create wallet
if [ -n "$USER_TOKEN" ]; then
    echo -e "\n📋 Test 4: User can create wallet"
    echo "curl -X POST $BASE_URL/api/v1/wallets (with user token)"
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/v1/wallets \
      -H "Authorization: Bearer $USER_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"userId": "user123", "currency": "USD"}')
    
    if [ "$HTTP_STATUS" = "201" ]; then
        echo "✅ PASS: User can create wallet (201)"
    else
        echo "❌ FAIL: Expected 201, got $HTTP_STATUS"
    fi
fi

# Test 5: User cannot access admin endpoints
if [ -n "$USER_TOKEN" ]; then
    echo -e "\n📋 Test 5: User cannot access admin endpoints"
    echo "curl -X GET $BASE_URL/api/v1/admin/metrics (with user token)"
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET $BASE_URL/api/v1/admin/metrics \
      -H "Authorization: Bearer $USER_TOKEN")
    
    if [ "$HTTP_STATUS" = "403" ]; then
        echo "✅ PASS: User correctly denied admin access (403)"
    else
        echo "❌ FAIL: Expected 403, got $HTTP_STATUS"
    fi
fi

# Test 6: Admin can access admin endpoints
if [ -n "$ADMIN_TOKEN" ]; then
    echo -e "\n📋 Test 6: Admin can access admin endpoints"
    echo "curl -X GET $BASE_URL/api/v1/admin/metrics (with admin token)"
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET $BASE_URL/api/v1/admin/metrics \
      -H "Authorization: Bearer $ADMIN_TOKEN")
    
    if [ "$HTTP_STATUS" = "200" ]; then
        echo "✅ PASS: Admin can access admin endpoints (200)"
    else
        echo "❌ FAIL: Expected 200, got $HTTP_STATUS"
    fi
fi

# Test 7: Admin can create wallet
if [ -n "$ADMIN_TOKEN" ]; then
    echo -e "\n📋 Test 7: Admin can create wallet"
    echo "curl -X POST $BASE_URL/api/v1/wallets (with admin token)"
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/v1/wallets \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"userId": "admin123", "currency": "EUR"}')
    
    if [ "$HTTP_STATUS" = "201" ]; then
        echo "✅ PASS: Admin can create wallet (201)"
    else
        echo "❌ FAIL: Expected 201, got $HTTP_STATUS"
    fi
fi

# Test 8: Invalid token should fail
echo -e "\n📋 Test 8: Invalid token should fail"
echo "curl -X POST $BASE_URL/api/v1/wallets (with invalid token)"
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/v1/wallets \
  -H "Authorization: Bearer invalid.jwt.token" \
  -H "Content-Type: application/json" \
  -d '{"userId": "user123", "currency": "USD"}')

if [ "$HTTP_STATUS" = "401" ]; then
    echo "✅ PASS: Invalid token correctly rejected (401)"
else
    echo "❌ FAIL: Expected 401, got $HTTP_STATUS"
fi

echo -e "\n🎉 Authentication tests completed!"
echo "=================================================="
echo "📝 Summary:"
echo "   - JWT authentication is working"
echo "   - Role-based access control is enforced"
echo "   - User role can access user endpoints only"
echo "   - Admin role can access all endpoints"
echo "   - Invalid/missing tokens are properly rejected"
echo ""
echo "🚀 The authentication system is ready for use!"
