-- Create test wallets
INSERT INTO wallets (id, userId, balance, status, createdAt, updatedAt) 
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'test-user-1', 1000.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO wallets (id, userId, balance, status, createdAt, updatedAt) 
VALUES ('550e8400-e29b-41d4-a716-446655440001', 'test-user-2', 500.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Create test transactions
INSERT INTO transaction (id, walletId, type, amount, referenceId, status, description, createdAt) 
VALUES ('550e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440000', 'DEPOSIT', 1000.00, 'DEP-001', 'COMPLETED', 'Initial deposit', CURRENT_TIMESTAMP);

INSERT INTO transaction (id, walletId, type, amount, referenceId, status, description, createdAt) 
VALUES ('550e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440001', 'DEPOSIT', 500.00, 'DEP-002', 'COMPLETED', 'Initial deposit', CURRENT_TIMESTAMP);

-- Transaction history is stored in DynamoDB, not MySQL
