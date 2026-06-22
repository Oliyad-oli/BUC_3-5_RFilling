# Tax Filing Core - Backend

Clean implementation of BUC-003 (Return Filing) and BUC-005 (Return Processing) following Hexagonal Architecture.

## Features

### BUC-003: Return Filing
- Register Daily Return (manual entry + e-invoice integration)
- Register Manual Receipt
- Submit Monthly Return
- Calculation engine integration
- Ledger posting (event-driven)

### BUC-005: Return Processing
- Officer review queue
- Risk assessment integration
- Rule validation integration
- Decision workflow (CLEAR / REQUEST_AMENDMENT / CONFIRM_FRAUD)

## Architecture

**Pattern**: Hexagonal Architecture (Ports & Adapters)  
**Design**: Domain-Driven Design (DDD)  
**Approach**: CQRS + Event Sourcing + Outbox Pattern

### Package Structure

```
com.itas.taxfiling
├── api/                      # REST API Layer
│   ├── controller/           # REST controllers
│   ├── dto/                  # Request/Response DTOs
│   ├── advice/               # Exception handlers
│   └── webhook/              # Webhook endpoints
├── application/              # Application Layer
│   ├── usecase/              # Use cases (commands/queries)
│   ├── port/                 # Port interfaces
│   ├── event/                # Event handlers
│   └── outbox/               # Outbox dispatcher
├── domain/                   # Domain Layer
│   ├── model/                # Aggregates and entities
│   ├── valueobject/          # Value objects
│   ├── event/                # Domain events
│   └── exception/            # Domain exceptions
├── persistence/              # Persistence Layer
│   ├── jpa/                  # JPA entities and repositories
│   └── adapter/              # Repository adapters
├── engineadapter/            # External integrations (mock)
├── observability/            # Cross-cutting concerns
└── config/                   # Spring configuration
```

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+ (or use H2 for dev)
- Kafka (optional, for event publishing)

## Setup

### 1. Database Setup

**PostgreSQL**:
```bash
createdb taxfiling
psql taxfiling < schema.sql
```

**Or use H2** (in-memory):
```bash
# No setup needed, runs with dev profile
```

### 2. Build

```bash
mvn clean install
```

### 3. Run

**Development mode** (H2 database):
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Production mode** (PostgreSQL):
```bash
mvn spring-boot:run
```

## API Documentation

Once running, access Swagger UI at:
```
http://localhost:8080/api/v1/swagger-ui.html
```

## API Endpoints

### Tax Returns (BUC-003)
```
POST   /api/v1/tax-returns/draft
PUT    /api/v1/tax-returns/{id}/draft
GET    /api/v1/tax-returns/{id}
GET    /api/v1/tax-returns?tin={tin}
POST   /api/v1/tax-returns/{id}/calculate
POST   /api/v1/tax-returns/{id}/submit
DELETE /api/v1/tax-returns/{id}
```

### Officer Review (BUC-005)
```
GET    /api/v1/cases?status=OPEN&assignedOfficer={id}
GET    /api/v1/cases/{id}
POST   /api/v1/cases/{id}/decision
GET    /api/v1/decisions?officer={id}
GET    /api/v1/officer/dashboard
```

### Filing Periods
```
GET    /api/v1/filing-periods?tin={tin}&status=OPEN,OVERDUE
```

### Authentication
```
POST   /api/v1/auth/login/taxpayer
POST   /api/v1/auth/login/officer
POST   /api/v1/auth/logout
POST   /api/v1/auth/refresh
```

## Testing

```bash
# Run all tests
mvn test

# Run integration tests only
mvn verify -P integration-test

# Run with coverage
mvn clean test jacoco:report
```

## Configuration

Edit `src/main/resources/application.yml` for:
- Database connection
- Kafka settings
- Outbox dispatcher settings
- Engine adapter mode (mock/real)

## Domain Model

### Aggregates
- **TaxReturn** - Core return filing aggregate
- **OfficerReviewItem** - Review queue item
- **FilingPeriod** - Filing period
- **OutboxEntry** - Event outbox entry

### Key Events
- TaxReturnDraftedEvent
- CalculationRequestedEvent
- CalculationAcceptedEvent
- PostedToLedgerEvent
- OfficerReviewItemCreatedEvent
- OfficerReviewDecidedEvent

## External Integrations (Mock Adapters)

All external integrations use mock implementations initially:
- **E-Invoice Service** - Pre-populate return data
- **Ledger Engine** - Post tax liability
- **Risk Engine** - Risk assessment
- **Rule Engine** - Validation and calculation
- **Workflow Engine** - Review routing
- **Notification Engine** - User notifications

To switch to real adapters, update `application.yml`:
```yaml
taxfiling:
  engine-adapters:
    mode: real
```

## Development

### Adding a New Use Case

1. Create domain event in `domain/event/`
2. Create use case in `application/usecase/`
3. Create DTO in `api/dto/`
4. Add controller endpoint in `api/controller/`
5. Add tests

### Database Migrations

Create new migration in `src/main/resources/db/migration/`:
```
V{version}__{description}.sql
```

Example: `V1__create_tax_return_tables.sql`

## Troubleshooting

### Database connection failed
- Check PostgreSQL is running: `pg_isready`
- Check credentials in `application.yml`

### Kafka connection failed
- Kafka is optional for development
- Set `spring.kafka.enabled=false` to disable

### Port already in use
- Change port in `application.yml`: `server.port`

## License

Internal use only - ITAS Tax Filing System
