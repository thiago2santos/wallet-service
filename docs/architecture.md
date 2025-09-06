# System Architecture

> Comprehensive guide to the Wallet Service architecture and design patterns

## 🎯 Overview

The Wallet Service is designed as a **cloud-native, event-driven microservice** following modern architectural patterns. Built with **CQRS**, **Event Sourcing**, and **Reactive Programming**, it delivers high performance and scalability for financial applications.

### 🏗️ Architectural Principles

- **🔄 CQRS** - Separate read and write models for optimal performance
- **📝 Event Sourcing** - Complete audit trail with event-driven state changes  
- **⚡ Reactive Programming** - Non-blocking, high-concurrency operations
- **🌐 Cloud Native** - Designed for containerized, distributed environments

- **📊 Observability** - Comprehensive monitoring and tracing

## System Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        API[Load Balancer]
        GATEWAY[API Gateway]
    end

    subgraph "Application Layer"
        SVC[Wallet Service<br/>Quarkus + Java 21]
        RESILIENCE[Resilience Layer<br/>Circuit Breakers + Retries]
        CACHE[Redis Cache]
        OUTBOX[Outbox Pattern<br/>Event Reliability]
    end

    subgraph "Data Layer"
        DB_PRIMARY[(MySQL Primary<br/>Write Operations)]
        DB_REPLICA[(MySQL Replica<br/>Read Operations)]
    end

    subgraph "Event Processing"
        KAFKA[Apache Kafka<br/>Event Streaming]
        ZOOKEEPER[Zookeeper<br/>Coordination]
        SCHEMA[Schema Registry<br/>Avro Schemas]
    end

    subgraph "Monitoring & Observability"
        PROMETHEUS[Prometheus<br/>Metrics]
        GRAFANA[Grafana<br/>Dashboards]
        KAFKA_UI[Kafka UI<br/>Management]
    end

    API --> GATEWAY
    GATEWAY --> SVC
    SVC --> RESILIENCE
    RESILIENCE --> CACHE
    RESILIENCE --> DB_PRIMARY
    RESILIENCE --> DB_REPLICA
    RESILIENCE --> KAFKA
    RESILIENCE --> OUTBOX
    OUTBOX --> KAFKA
    KAFKA --> ZOOKEEPER
    KAFKA --> SCHEMA
    
    PROMETHEUS --> SVC
    PROMETHEUS --> RESILIENCE
    GRAFANA --> PROMETHEUS
    KAFKA_UI --> KAFKA
```

## Transaction Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant API as API Gateway
    participant WS as Wallet Service
    participant RES as Resilience Layer
    participant Cache as Redis Cache
    participant DB_W as MySQL Primary
    participant DB_R as MySQL Replica
    participant OUTBOX as Outbox Table
    participant KAFKA as Apache Kafka

    C->>API: Request Transaction
    API->>WS: Forward Request
    WS->>RES: Execute with Resilience
    
    alt Cache Available
        RES->>Cache: Check Cached Balance
    else Cache Circuit Open
        RES->>DB_R: Direct Database Query
    end
    
    RES->>DB_W: Begin Transaction
    RES->>DB_W: Update Wallet Balance
    RES->>DB_W: Create Transaction Record
    
    alt Kafka Available
        RES->>KAFKA: Publish Transaction Event
    else Kafka Circuit Open
        RES->>OUTBOX: Store Event for Later
    end
    
    RES->>DB_W: Commit Transaction
    
    alt Cache Available
        RES->>Cache: Update Cache
    end
    
    WS-->>C: Success Response
    
    Note over OUTBOX: Events replayed when<br/>Kafka recovers
    Note over RES: Circuit breakers prevent<br/>cascade failures
```

## Core Components

### API Layer
- **Load Balancer**
  - Traffic distribution
  - Health checks
  - SSL termination
  - High availability

- **API Gateway**
  - RESTful API endpoints
  - Request/response validation
  - Rate limiting

### Application Layer
- **Quarkus Application**
  - Reactive programming with Mutiny
  - Native compilation support
  - Sub-second startup time
  - Low memory footprint (~100MB)
  - CQRS architecture implementation

- **Redis Cache**
  - Balance caching (sub-ms latency)
  - Session management
  - Rate limiting counters
  - Distributed locking

### Data Layer
- **MySQL Primary**
  - Write operations
  - ACID compliance
  - Transaction consistency
  - Automated backups

- **MySQL Replica**
  - Read operations
  - Query performance
  - Load distribution
  - High availability

### Event Processing
- **Apache Kafka**
  - Event streaming
  - Durable message storage
  - Partition-level ordering
  - High throughput (millions msg/sec)

- **Zookeeper**
  - Kafka cluster coordination
  - Configuration management
  - Leader election
  - Service discovery

- **Schema Registry**
  - Avro schema management
  - Schema evolution
  - Compatibility checking
  - Serialization optimization

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
