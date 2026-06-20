# Tax Filing Core Server

**ITAS Platform — BUC-003 Return Filing & BUC-005 Return Processing**

---

## Overview

`tax-filing-core-server` is the authoritative service for tax return filing and processing within the ITAS platform. It implements a strict hexagonal architecture (ports & adapters) with Domain-Driven Design.

| BUC | Name | Description |
|---|---|---|
| BUC-003 | Return Filing | Draft, calculate, submit, and amend tax returns |
| BUC-005 | Return Processing | Post-ledger validation, officer review, fraud detection, certificate issuance |

---

## Architecture

```
com.itas.taxfiling/
├── api/             ← REST controllers, DTOs, RFC-7807 error handling
├── application/     ← Use cases, ports (interfaces), event handlers, outbox, scheduling
├── domain/          ← Aggregates, value objects, domain events (NO Spring/JPA annotations)
├── persistence/     ← JPA entities, Spring Data repos, persistence adapters
├── engineadapter/   ← External system adapters (ledger, risk, rule, e-invoice, tax-type…)
└── observability/   ← AuditInterceptor (AOP), MdcContextFilter
```

**Security**: Intentionally absent. Authentication and authorization are owned entirely by the API Gateway + Keycloak. The service receives identity via the `X-Actor-Id` header.

---

## Stack

| Technology | Version |
|---|---|
| Java | 21 (virtual threads enabled) |
| Spring Boot | 3.3.0 |
| PostgreSQL | 16 |
| Flyway | managed |
| Hibernate | 6 (`ddl-auto: validate`) |
| Resilience4j | 2.1.0 |
| Springdoc OpenAPI | 2.0.4 |
| Lombok | 1.18.46 |
| MapStruct | 1.5.5.Final |
| ArchUnit | 1.3.0 |
| Testcontainers | 1.19.2 |

---

## Quick Start

```bash
# Prerequisites: Java 21, Maven 3.9+, PostgreSQL 16 running

# 1. Create the database
psql -U postgres -c "CREATE DATABASE tax_filing_db;"

# 2. Run the application (Flyway applies migrations automatically)
mvn spring-boot:run

# 3. Open Swagger UI
open http://localhost:8081/api/v1/swagger-ui.html

# 4. Run all tests
mvn test

# 5. Run integration tests (requires Docker)
mvn verify
```

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8081` | HTTP port |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `tax_filing_db` | Database name |
| `DB_USER` | `root` | Database username |
| `DB_PASS` | `root@1234` | Database password |
| `LEDGER_BASE_URL` | `http://172.16.0.92:32703` | Ledger engine base URL |

---

## API Endpoints

All endpoints are under `http://host:8081/api/v1/`. See Swagger UI for full documentation.

| Path | Description | BUC |
|---|---|---|
| `POST /tax-returns` | Draft a new TaxReturn | BUC-003 |
| `POST /filing-periods/{id}/start` | Start filing from obligation card | BUC-003 |
| `POST /tax-returns/{id}/schedules` | Add a schedule | BUC-003 |
| `POST /tax-returns/{id}/schedules/{sid}/line-items` | Add a line item | BUC-003 |
| `POST /tax-returns/{id}/calculate` | Run calculation loop | BUC-003 |
| `POST /tax-returns/{id}/iterations/{itid}/accept` | Accept calculation (submit) | BUC-003 |
| `POST /tax-returns/{id}/amendments` | Open an amendment | BUC-003 |
| `GET /tax-returns/{id}/details` | Full return details | BUC-003/005 |
| `GET /officer-review-items` | Officer review queue | BUC-005 |
| `POST /officer-review-items/{id}/decision` | Officer decision | BUC-005 |
| `GET /filing-certificates/by-tax-return/{id}` | Retrieve filing certificate | BUC-005 |
| `GET /portal/filing/dashboard` | Taxpayer dashboard | BUC-003 |

---

## Domain Model

```
TaxReturn (aggregate root)
├── Schedule[] (entity)
│   └── LineItem[] (entity)
├── CalculationIteration[] (entity)
├── Amendment (open, optional)
└── Amendment[] (historical)

FilingPeriod (aggregate root)
TaxpayerObligation (aggregate root)
LineItemEntryType (aggregate root)
OfficerReviewItem (aggregate root)
FilingCertificate (aggregate root)
OutboxEntry (aggregate root)
```

---

## Testing

```bash
# Unit tests (no Spring context, no DB)
mvn test

# Integration tests (Testcontainers PostgreSQL)
mvn verify

# ArchUnit architecture enforcement
mvn test -Dtest=ArchitectureTest

# Single use case test
mvn test -Dtest=AcceptCalculationUseCaseTest
```

---

*Derived from `bs-filing-core-server` — optimized exclusively for BUC-003 & BUC-005.*
