-- Outbox table for transactional event publishing (replica)
-- This ensures events are published reliably using the Outbox Pattern

CREATE TABLE IF NOT EXISTS outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSON NOT NULL,
    created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),
    processed_at TIMESTAMP(6) NULL,
    version INT DEFAULT 1,
    
    -- Indexes for efficient querying
    INDEX idx_outbox_unprocessed (processed_at, created_at),
    INDEX idx_outbox_aggregate (aggregate_id, created_at),
    INDEX idx_outbox_type (event_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
