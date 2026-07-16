# Cross-Repo Code-Review Scenarios — Ground Truth

Test fixtures for evaluating Niro / Devin / Greptile cross-repo understanding.
3 repos, one lending domain: `portal-bff` (TS/NestJS) → `loan-service` (Java) → `risk-service` (Java).

Each scenario is marked:
- **TRUE** — a real connection the tool SHOULD detect.
- **FALSE** — a name-only coincidence the tool should NOT link (precision trap).
- **ORPHAN** — a producer/endpoint with no counterpart.

Line numbers are indicative (source evolves); grep the marked symbol to relocate.

---

## 1. Cross-repo HTTP calls (TRUE edges)

| Caller | Client lib | → Target endpoint | Target handler |
|--------|-----------|-------------------|----------------|
| `portal-bff` `LoanClient.createLoan` (`src/clients/loan.client.ts:20`) | axios | `POST /api/v1/loans` | `loan-service` `LoanController.apply` (`controller/LoanController.java:24`) |
| `portal-bff` `LoanClient.getLoan` (`src/clients/loan.client.ts:25`) | axios | `GET /api/v1/loans/{id}` | `loan-service` `LoanController.getLoan` (`:29`) |
| `portal-bff` `RiskClient.getRisk` (`src/clients/risk.client.ts:13`) | fetch | `GET /api/v1/risk/{id}` | `risk-service` `RiskController.getRisk` (`controller/RiskController.java:28`) |
| `loan-service` `RiskClient.assess` (`client/RiskClient.java:16`) | OpenFeign | `POST /api/v1/risk/assess` | `risk-service` `RiskController.assess` (`:23`) |
| `loan-service` `RiskWebClient.getRisk` (`client/RiskWebClient.java`) | WebClient | `GET /api/v1/risk/{id}` | `risk-service` `RiskController.getRisk` (`:28`) |
| `loan-service` `LoanCallbackClient.fetchLoan` (`client/LoanCallbackClient.java`) | RestTemplate | `GET /api/v1/loans/{id}` | `loan-service` `LoanController.getLoan` (`:29`) |

**Same target, two callers, different languages/libs:** `GET /api/v1/risk/{id}` is called by BOTH loan-service (WebClient) and portal-bff (fetch). A good tool links both callers to the one handler.

## 2. Same HTTP endpoint path in multiple repos (FALSE traps)

| Path | loan-service | risk-service | portal-bff |
|------|-------------|--------------|------------|
| `GET /api/v1/health` | `controller/HealthController.java:12` | `controller/HealthController.java:15` | `controllers/health.controller.ts:9` |
| `POST /api/v1/status` | `controller/StatusController.java:13` | `controller/StatusController.java:15` | — |

**FALSE** — identical paths, three/two unrelated handlers, no call edge between them. A tool must NOT report these as the "same" or "connected" endpoint. Contrast with §1 where the same path IS a real caller→handler edge.

## 3. Proxy endpoints (four kinds)

| Kind | Endpoint | Forwards to | Location |
|------|----------|-------------|----------|
| Pass-through | `GET /proxy/loans/:id` | loan-service `GET /api/v1/loans/{id}` verbatim | `portal-bff/src/controllers/proxy.controller.ts:13` |
| Transforming | `GET /portal/loans/:id` | loan-service loan → reshaped `PortalLoanView` | `portal-bff/src/controllers/portal.controller.ts:17` + `services/aggregation.service.ts` `transform` |
| Aggregating | `GET /portal/dashboard/:userId` | loan-service + risk-service, merged | `portal-bff/src/controllers/portal.controller.ts:23` → `aggregation.service.ts` `buildDashboard` |
| Server-side Java proxy | `GET /api/v1/risk-proxy/{id}` | risk-service `GET /api/v1/risk/{id}` via WebClient | `loan-service/.../controller/RiskProxyController.java:25` |

## 4. Kafka events

| Topic | Broker | Producer | Consumer | Type |
|-------|--------|----------|----------|------|
| `loan.applied` | **broker-a:9092** | loan-service `LoanEventProducer` `brokerATemplate.send` (`kafka/LoanEventProducer.java:29`) | risk-service `RiskEventConsumer` `@KafkaListener` (`kafka/RiskEventConsumer.java:24`) | **TRUE** shared |
| `loan.applied` | **broker-b:9092** | loan-service `LoanEventProducer` `brokerBTemplate.send` (`kafka/LoanEventProducer.java:30`) | (none) | **FALSE** — same topic name, different broker |
| `risk.assessed` | broker-a:9092 | risk-service `RiskEventProducer.publishRiskAssessed` (`kafka/RiskEventProducer.java:21`) | loan-service `RiskAssessedConsumer` (`kafka/RiskAssessedConsumer.java:13`) | **TRUE** shared |
| `loan.rejected` | broker-a:9092 | risk-service `RiskEventProducer.publishLoanRejected` (`kafka/RiskEventProducer.java:26`) | (none) | **ORPHAN** |

**Multiple brokers, same repo:** loan-service defines both `brokerATemplate` and `brokerBTemplate` (`config/KafkaConfig.java:37,42`) and publishes `loan.applied` on BOTH. Only the broker-a copy is truly shared with risk-service.

## 5. DB resources

| Schema | Host | DataSource | Tables | Type |
|--------|------|-----------|--------|------|
| `lending` | **db-primary:5432** | loan-service default (`application.yml`), risk-service `primaryDataSource` (`config/PrimaryDataSourceConfig.java`) | `loans`, `audit_log` | **TRUE** shared |
| `lending` | **db-secondary:5432** | risk-service `reportingDataSource` (`config/ReportingDataSourceConfig.java`) | `risk_scores`, `audit_log` | **FALSE** — same schema name, different host |

- `loans` (`loan-service Loan.java`) on db-primary/lending — read/written by loan-service; risk-service's primaryDataSource points at the same physical schema.
- **Two datasources in one repo:** risk-service wires `primaryDataSource` + `reportingDataSource`, both schema name `lending`, different hosts.
- **Table-name collision:** `audit_log` mapped in loan-service (`entity/AuditLog.java`), risk-service primary (`entity/AuditLog.java`) and reporting side — same name across DBs.

## 6. Method overloading / overriding

| Kind | Location |
|------|----------|
| Java overloading (3×) | risk-service `RiskService.assessRisk(Long)`, `(Long,String)`, `(LoanDto)` — `service/RiskService.java` |
| Java overriding | risk-service `DefaultRiskAssessor` overrides `RiskAssessor.assess` + `toString` — `service/DefaultRiskAssessor.java` |
| TS overloading | portal-bff `AggregationService.merge` two overload signatures + union impl — `services/aggregation.service.ts` |

## 7. Cross-transport variety

| Transport | Client | Server |
|-----------|--------|--------|
| gRPC | loan-service `RiskGrpcClient` `@GrpcClient("risk-service")` (`grpc/RiskGrpcClient.java:16`) | risk-service `RiskGrpcService` `@GrpcService` (`grpc/RiskGrpcService.java:16`) |
| GraphQL | portal-bff `DashboardResolver` `@Query dashboard` (`resolvers/dashboard.resolver.ts`) → fan-out to both Java REST services | (aggregating, same as §3) |

Proto `risk.proto` is COPIED into both Java repos (`*/src/main/proto/risk.proto`) — not a shared artifact.

## 8. False-positive traps (extra)

| Trap | loan-service | risk-service |
|------|-------------|--------------|
| Same class `LoanDto`, different package | `loan/dto/LoanDto.java` (`...loan.dto`) | `risk/dto/LoanDto.java` (`...risk.dto`) |
| Same event POJO copied | `loan/kafka/LoanAppliedEvent.java` | `risk/kafka/LoanAppliedEvent.java` |
| Same bean name `auditPublisher`, different impl | `config/AuditConfig.java` → `KafkaAuditPublisher` | `config/AuditConfig.java` → `DbAuditPublisher` |
| Same config key, different value | `lending.risk.threshold: 0.7` (`application.yml`) | `lending.risk.threshold: 0.5` (`application.yml`) |

None of these are real shared code — copies/coincidences only.

---

## Build / verify

```bash
# risk-service, loan-service
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
(cd risk-service && mvn -q compile)
(cd loan-service && mvn -q compile)
# portal-bff
(cd portal-bff && npm install && npx tsc --noEmit)
```

Each repo is its own git repo. No services run; fixtures are for static indexing only.
