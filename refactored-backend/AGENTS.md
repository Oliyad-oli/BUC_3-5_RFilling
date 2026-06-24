# tax-filing-core-server — Agent Instructions

This file is the canonical "how to work in this repo" guide for any AI agent.
Read it before making changes.

---

## 1. What this service does

**Tax Filing Core Server** — the ITAS platform service for BUC-003 and BUC-005.

- **BUC-003 Return Filing**: TaxReturn lifecycle from DRAFT → ACCEPTED, including schedules, line items, calculation loop, amendments, e-invoice pull, and bulk upload.
- **BUC-005 Return Processing**: Post-acceptance flow — ledger posting (via outbox), parallel risk + rule validation, officer review queue, fraud flagging, filing certificate issuance.

Out of scope (owned by other services): reminders, payment processing, refunds, VAT refund lifecycle, income estimation, non-filer compliance, municipality bills, fraud investigation post-confirmation, extension requests.

---

## 2. Build / run / test

```bash
# Run all unit tests
mvn test

# Run unit + Testcontainers integration tests
mvn verify

# Boot locally (listens on 8081)
mvn spring-boot:run

# Verify ArchUnit rules only
mvn test -Dtest=ArchitectureTest
```

**Java 21** is required. Surefire argLine carries `-Dnet.bytebuddy.experimental=true`.

---

## 3. Architecture rules (non-negotiable)

### 3.1 Six-layer hexagonal layout
```
api/  →  application/  →  domain/  ←  persistence/, engineadapter/
                          ↑
                       (no inbound deps)
```

Root package: `com.itas.taxfiling`

- **No `security/` package.** Security is owned by the API gateway + Keycloak. There is NO `spring-boot-starter-security` on the classpath. Do not add `@PreAuthorize`, `@Secured`, or any Spring Security import.
- ArchUnit in `ArchitectureTest.java` enforces all layer dependency rules.

### 3.2 Aggregates + events (DDD)
- Every write use case ends with `saved.pullEvents().forEach(eventPublisher::publish)` after `save()`.
- Aggregates: `TaxReturn`, `Schedule`, `LineItem`, `LineItemEntryType`, `OfficerReviewItem`, `FilingCertificate`, `OutboxEntry`, `FilingPeriod`, `TaxpayerObligation`.
- Value objects are Java records in `domain/valueobject/` — standalone top-level types only.
- Domain methods register events; the use case drains them.

### 3.3 URL convention
- `server.servlet.context-path: /api/v1` provides the prefix.
- **Controllers do NOT repeat `/api/v1`** in their `@RequestMapping`.
- All endpoints are therefore reachable at `http://host:8081/api/v1/<path>`.

### 3.4 Persistence rules
- Hibernate is configured `ddl-auto: validate`. Schema is owned by Flyway.
- **Never edit a Flyway migration that has been committed.** Add a new `VNN__describe_change.sql`.
- Persistence adapters do NOT carry `@Transactional` — the use case owns the transaction.
- JSONB columns use `@Convert(converter = JsonbConverter.class)`.
- For entities with `@Version`, preserve the version when mapping domain → entity.

### 3.5 API rules
- No auth annotations anywhere — gateway owns auth.
- Request DTOs own `toDomain()`; response DTOs own `static from(DomainObject)`.
- Controllers are pure orchestration — no business logic.
- Errors via RFC 7807 `ProblemDetail`. Engine failure → `502`. Domain rule → `422`. Not found → `404`. Validation → `400`.
- `@Tag` on every controller class; `@Operation` on every endpoint.

### 3.6 Resilience
Every engine adapter method has `@CircuitBreaker(name = "engine-name", fallbackMethod = "...")` and `@Retry(name = "engine-name")`.

---

## 4. Testing rules

- One unit test per use case. `@ExtendWith(MockitoExtension.class)`, no Spring context.
- Cover happy path + at least 1 error path + key state assertion.
- IT tests use Testcontainers PostgreSQL; skip gracefully when Docker is absent.
- ArchUnit test enforces 5 mandatory rules.
- **Always update tests when you change something.**

---

## 5. Common command snippets

```bash
# Boot locally
mvn spring-boot:run

# Run a single test class
mvn test -Dtest=AcceptCalculationUseCaseTest

# Run integration tests
mvn verify

# Verify architecture
mvn test -Dtest=ArchitectureTest
```

---

## 6. When adding a new endpoint

1. Add the use case in `application/usecase/<area>/`.
2. Add request + response DTOs (request DTOs have `toDomain()`, response DTOs have `static from(...)`).
3. Add a controller method (no business logic — pure orchestration).
4. Add a unit test for the use case.
5. Run `mvn test`.
6. If you touched persistence, add a Flyway migration.

---

## 7. BUC traceability

| BUC | Description | Key Use Cases |
|---|---|---|
| BUC-003 | Return Filing | `CreateTaxReturnUseCase`, `AddScheduleUseCase`, `AddLineItemUseCase`, `RequestCalculationUseCase`, `AcceptCalculationUseCase`, `RequestAmendmentUseCase` |
| BUC-005 | Return Processing | `RecordLedgerPostedUseCase`, `StartPostLedgerValidationUseCase`, `RecordRiskOutcomeUseCase`, `RecordRuleOutcomeUseCase`, `OfficerClearTaxReturnUseCase`, `OfficerConfirmFraudUseCase` |
