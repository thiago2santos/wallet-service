# Architecture Design

## Overview

The Wallet Service is designed following AWS Well-Architected Framework principles, ensuring high availability, scalability, and security for financial transactions.

## System Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        API[API Gateway]
        WAF[AWS WAF]
    end

    subgraph "Application Layer"
        SVC[Wallet Service]
        CACHE[ElastiCache Redis]
    end

    subgraph "Data Layer"
        DB[(Amazon Aurora MySQL)]
        DDB[(DynamoDB)]
    end

    subgraph "Event Processing"
        SQS[SQS FIFO Queue]
        SNS[SNS Topic]
        DLQ[Dead Letter Queue]
    end

    subgraph "Monitoring & Observability"
        CW[CloudWatch]
        XRay[X-Ray]
        CT[CloudTrail]
    end

    WAF --> API
    API --> SVC
    SVC --> CACHE
    SVC --> DB
    SVC --> DDB
    SVC --> SQS
    SQS --> DLQ
    SQS --> SNS
    
    CW --> SVC
    XRay --> SVC
    CT --> SVC
```

## Transaction Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant API as API Gateway
    participant WS as Wallet Service
    participant Cache as Redis
    participant DB as Aurora
    participant Q as SQS FIFO
    participant SNS as SNS Topic

    C->>API: Request Transaction
    API->>WS: Forward Request
    WS->>Cache: Check Balance
    WS->>DB: Begin Transaction
    WS->>DB: Update Balance
    WS->>Q: Send Transaction Event
    WS->>DB: Commit Transaction
    WS-->>C: Response
    Q->>SNS: Publish Event
```

## Core Components

### API Layer
- **AWS API Gateway**
  - RESTful API endpoints
  - Request/response transformation
  - API versioning
  - Throttling controls

- **AWS WAF**
  - DDoS protection
  - SQL injection prevention
  - Rate limiting
  - IP blocking

### Application Layer
- **Quarkus Application**
  - Reactive programming model
  - Native compilation support
  - Fast startup time
  - Low memory footprint

- **ElastiCache Redis**
  - Balance caching
  - Rate limiting
  - Distributed locking
  - Session management

### Data Layer
- **Amazon Aurora MySQL**
  - Primary transaction database
  - ACID compliance
  - Automated backups
  - Read replicas

- **Amazon DynamoDB**
  - Historical data storage
  - Audit logging
  - High-speed queries
  - Auto-scaling

### Event Processing
- **Amazon SQS FIFO**
  - Ordered message delivery
  - Exactly-once processing
  - Dead letter queues
  - Message retention

- **Amazon SNS**
  - Event notifications
  - Multiple subscribers
  - Message filtering
  - Delivery retries

## Scalability & Performance

### Horizontal Scaling
- Auto-scaling groups for application layer
- Read replicas for Aurora
- On-demand scaling for DynamoDB

### Caching Strategy
- Redis for hot data
- Write-through caching
- Cache invalidation patterns
- TTL management

### Performance Optimization
- Connection pooling
- Query optimization
- Batch processing
- Asynchronous operations

## High Availability & Disaster Recovery

### Multi-AZ Deployment
- Active-active configuration
- Automated failover
- Load balancing
- Health checks

### Backup & Recovery
- Continuous backup for Aurora
- Point-in-time recovery
- Cross-region replication
- Regular DR testing

## Monitoring & Alerting

### Metrics Collection
- Custom CloudWatch metrics
- Performance monitoring
- Error tracking
- Business metrics

### Alerting Strategy
- Multi-level alerting
- PagerDuty integration
- Automated responses
- Escalation policies

## Cost Optimization

### Resource Management
- Auto-scaling policies
- Right-sizing instances
- Reserved instances
- Spot instances where applicable

### Cost Monitoring
- Budget alerts
- Usage tracking
- Cost allocation tags
- Regular optimization reviews
