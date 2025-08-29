-- Create test wallets for testing
INSERT INTO wallet (id, user_id, currency, balance, status, created_at, updated_at) 
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'test-user-1', 'USD', 1000.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Create test transactions for testing
INSERT INTO transaction (id, wallet_id, type, amount, reference_id, status, description, created_at) 
VALUES ('550e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440000', 'DEPOSIT', 1000.00, 'DEP-001', 'COMPLETED', 'Initial deposit', CURRENT_TIMESTAMP);

-- Create transaction history records for testing
INSERT INTO transaction_history (id, wallet_id, balance, timestamp, transaction_id) 
VALUES ('550e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440000', 1000.00, CURRENT_TIMESTAMP, '550e8400-e29b-41d4-a716-446655440002');
