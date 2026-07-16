# Code Review Test Fixture Repos — Design

**Date:** 2026-07-16
**Purpose:** Build 3 interconnected repos (2 Java Spring Boot, 1 TypeScript NestJS) that
deliberately exercise cross-repo and intra-repo code-review scenarios. Used to evaluate
how Niro, Devin, and Greptile detect cross-repo connections, shared resources, event
flows, and code-structure patterns. Repos are indexed by those tools after creation.

**Design goal:** Every scenario appears in BOTH a *true* form (a real connection the tool
SHOULD detect) and, where meaningful, a *false* form (a name-only coincidence the tool
should NOT link). This measures precision, not just recall.

## Repos

| Repo | Lang | Framework | Role | Git |
|------|------|-----------|------|-----|
| `loan-service` | Java 17 | Spring Boot 3 (Web, Kafka, JPA, gRPC, OpenFeign) | Core lending. Applies loans, orchestrates risk. | own `git init` |
| `risk-service` | Java 17 | Spring Boot 3 (Web, Kafka, JPA, gRPC) | Risk scoring. Event consumer/producer. | own `git init` |
| `portal-bff` | TypeScript | NestJS | Frontend BFF. Proxies + aggregates both Java services. GraphQL + REST. | own `git init` |

Java base package: `com.example.lending.<service>`.
Build fidelity: compilable (pom.xml / package.json, correct imports). No running or tests required.

**Domain flow:** `portal-bff` → `loan-service` (apply loan) → `risk-service` (assess risk).
Events + callbacks flow back.

## Scenario Matrix

### 1. Cross-repo HTTP calls (multiple client libs)
- `portal-bff` (axios) → `loan-service` `POST /api/v1/loans`
- `loan-service` (OpenFeign `RiskClient`) → `risk-service` `POST /api/v1/risk/assess`
- `loan-service` (WebClient) → `risk-service` `GET /api/v1/risk/{id}`
- `risk-service` (RestTemplate) → `loan-service` `GET /api/v1/loans/{id}` (callback — bidirectional edge)
- `portal-bff` (fetch) → `risk-service` `GET /api/v1/risk/{id}` (BFF talks to both)

### 2. Same HTTP endpoint path in multiple repos (over-link trap)
- `GET /api/v1/health` — implemented in loan-service, risk-service, AND portal-bff (3 handlers, same path, no real link)
- `POST /api/v1/status` — in loan-service AND risk-service, different behavior
- `GET /api/v1/loans/{id}` — real target of risk-service callback vs same path being a coincidence elsewhere
- Test: does the tool distinguish a real HTTP caller→handler edge from same-path coincidence?

### 3. Proxy endpoints (four kinds)
- **Pass-through**: BFF `GET /proxy/loans/:id` → forwards raw bytes to loan-service
- **Transforming**: BFF `GET /portal/loans/:id` → reshapes loan-service response into portal DTO
- **Aggregating**: BFF `GET /portal/dashboard/:userId` → fans out to loan-service + risk-service, merges
- **Server-side Java proxy**: loan-service `GET /api/v1/risk-proxy/{id}` → proxies risk-service `GET /api/v1/risk/{id}`

### 4. Kafka events
- **True shared (SHOULD link)**: topic `loan.applied`, broker `broker-a:9092` — produced in loan-service, consumed in risk-service
- **False match (should NOT link)**: topic `loan.applied`, broker `broker-b:9092` — second producer config, same topic name, different broker
- **Same repo, multiple brokers**: loan-service defines two `KafkaTemplate` beans (`brokerATemplate`, `brokerBTemplate`) both publishing topic `loan.applied`
- **Real return event**: topic `risk.assessed`, broker-a — produced in risk-service, consumed in loan-service
- **Orphan event trap**: topic `loan.rejected` produced in risk-service, no consumer anywhere

### 5. DB resources
- **True shared (SHOULD link)**: schema `lending` on `db-primary:5432`, table `loans` — read/written by both Java repos
- **False match (should NOT link)**: schema `lending` on `db-secondary:5432` — same schema name, different host, separate DataSource
- **Multiple datasources one repo**: risk-service wires `primaryDataSource` (db-primary) + `reportingDataSource` (db-secondary), both schema `lending`
- **Shared table name**: `audit_log` table exists in both DBs (name collision, not shared)

### 6. Method overloading / overriding
- **Java overloading**: `RiskService.assessRisk(Long id)`, `assessRisk(Long id, String tier)`, `assessRisk(LoanApplication app)` — 3 overloads
- **Java overriding**: interface `RiskAssessor` with impl overriding `assess(...)`; overridden `toString()`
- **TS overloads**: NestJS service method with TypeScript overload signatures + a union-typed variant

### 7. Cross-transport variety
- **gRPC**: loan-service (gRPC client) → risk-service (gRPC server) `RiskGrpc.assess` — proto shared by copy
- **GraphQL**: portal-bff exposes GraphQL `query dashboard(userId)` resolving via aggregating proxy to both Java REST services

### 8. False-positive traps (extra)
- **Same class name, different package**: `LoanDto` in `com.example.lending.loan.dto` AND `com.example.lending.risk.dto` (copies, not shared)
- **Same event POJO copied**: `LoanAppliedEvent` duplicated in both Java repos (not a shared lib)
- **Same Spring bean name**: bean `auditPublisher` defined in both Java repos, different implementations
- **Same config key, different value**: `lending.risk.threshold` = `0.7` in loan-service, `0.5` in risk-service

## File Layout

**Each Java repo:**
```
<svc>/
  pom.xml
  src/main/resources/application.yml
  src/main/proto/risk.proto            (gRPC repos)
  src/main/java/com/example/lending/<svc>/
    <Svc>Application.java
    controller/    REST controllers
    service/       business logic (overloads, overrides)
    client/        Feign/WebClient/RestTemplate cross-repo callers
    kafka/         producers + consumers (multi-broker config)
    grpc/          gRPC client or server
    repository/    JPA repositories
    entity/        JPA entities (loans, audit_log)
    config/        DataSource beans, KafkaTemplate beans, bean-name collisions
    dto/           DTOs (LoanDto collision)
```

**portal-bff (TS/NestJS):**
```
portal-bff/
  package.json  tsconfig.json  nest-cli.json
  src/
    main.ts  app.module.ts
    controllers/   REST + proxy endpoints
    resolvers/     GraphQL resolver
    services/      aggregation, transform (TS overloads)
    clients/       axios + fetch cross-repo callers
    dto/           portal DTOs
    schema.gql
```

## Non-goals
- No running services, no docker-compose, no integration tests.
- No shared library extraction — duplication is intentional (trap scenarios).
- No auth/security hardening — these are fixtures, not production.

## Success criteria
- All 3 repos compile (mvn compile / tsc).
- Each of the 8 scenario groups present and greppable.
- A companion `SCENARIOS.md` (or root README per repo) maps each scenario to exact
  file:line so results from Niro/Devin/Greptile can be scored against ground truth.
