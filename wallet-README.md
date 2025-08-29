# Wallet Service

A robust, scalable wallet service for managing users' digital money, built with Java, Quarkus, and AWS services.

## Overview

This wallet service is designed to handle high-volume financial transactions with strong consistency and reliability guarantees. It provides core functionality for digital wallet operations while maintaining strict security and compliance standards.

## Key Features

- Create and manage digital wallets
- Real-time balance inquiries
- Historical balance lookups
- Secure money deposits
- Controlled withdrawals
- Wallet-to-wallet transfers

## Architecture Overview

The service is built using a modern, cloud-native architecture:

- **Backend**: Java + Quarkus
- **Cloud Platform**: AWS
- **Primary Database**: Amazon Aurora MySQL
- **Cache Layer**: Amazon ElastiCache (Redis)
- **Event Processing**: Amazon SQS FIFO + SNS
- **API Gateway**: Amazon API Gateway + AWS WAF

For detailed architecture information, see [architecture.md](docs/architecture.md).

## Documentation

- [Architecture Design](docs/architecture.md)
- [API Documentation](docs/api.md)
- [Data Model](docs/data-model.md)
- [Security & Compliance](docs/security.md)

## Prerequisites

- Java 17 or higher
- Maven 3.8.1 or higher
- Docker
- AWS CLI configured with appropriate permissions

## Getting Started

1. Clone the repository
2. Configure AWS credentials
3. Run local development environment:
   ```bash
   ./mvnw quarkus:dev
   ```

## Contributing

Please read our contributing guidelines before submitting pull requests.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
