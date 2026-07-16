# Code Review Test Fixture Repos — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build 3 interconnected repos (loan-service + risk-service in Java Spring Boot, portal-bff in TS NestJS) that deliberately exercise cross-repo and intra-repo code-review scenarios for evaluating Niro/Devin/Greptile.

**Architecture:** Fintech lending. `portal-bff` → `loan-service` (apply) → `risk-service` (assess). Events + callbacks flow back. Every scenario appears in a TRUE form (real connection tools SHOULD detect) and, where meaningful, a FALSE form (name-only coincidence tools should NOT link).

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Kafka, Spring Data JPA, OpenFeign, WebFlux (WebClient), grpc-spring-boot-starter, PostgreSQL driver. TypeScript 5, NestJS 10, @nestjs/graphql, axios.

## Global Constraints

- Java base package: `com.example.lending.<service>` (loan / risk).
- Java 17, Spring Boot 3.2.x. Build tool: Maven (`pom.xml`).
- Verification = COMPILATION ONLY. Java: `mvn -q compile`. TS: `npx tsc --noEmit`. No running services, no unit/integration tests, no docker.
- Duplication across repos is INTENTIONAL (trap scenarios) — never extract shared libs.
- No auth/security hardening — fixtures only.
- Each repo is its OWN git repo (`git init` per folder). Root base dir: `/Users/divya.lunawat/Development/nisquare/code-review-repos`.
- All ground-truth scenario→file:line mappings recorded in `SCENARIOS.md` at root (Task 18).
- DB hosts (for config strings, never connected): `db-primary:5432`, `db-secondary:5432`, schema name `lending` on BOTH.
- Kafka brokers (config strings, never connected): `broker-a:9092`, `broker-b:9092`.

---

## Ground-Truth Reference (used across tasks)

**HTTP endpoints:**
| Path | Method | loan-service | risk-service | portal-bff |
|------|--------|:---:|:---:|:---:|
| `/api/v1/health` | GET | ✅ | ✅ | ✅ (same-path trap, no link) |
| `/api/v1/status` | POST | ✅ | ✅ | — (same-path trap) |
| `/api/v1/loans` | POST | ✅ (real target of BFF) | — | — |
| `/api/v1/loans/{id}` | GET | ✅ (real target of risk callback) | — | — |
| `/api/v1/risk/assess` | POST | — | ✅ (real target of Feign) | — |
| `/api/v1/risk/{id}` | GET | — | ✅ (real target of WebClient+fetch) | — |
| `/api/v1/risk-proxy/{id}` | GET | ✅ (server-side proxy → risk) | — | — |
| `/proxy/loans/:id` | GET | — | — | ✅ pass-through proxy |
| `/portal/loans/:id` | GET | — | — | ✅ transforming proxy |
| `/portal/dashboard/:userId` | GET | — | — | ✅ aggregating proxy |

**Kafka topics:**
| Topic | Broker | Producer | Consumer | Type |
|-------|--------|----------|----------|------|
| `loan.applied` | broker-a:9092 | loan-service | risk-service | TRUE shared (link) |
| `loan.applied` | broker-b:9092 | loan-service (2nd template) | — | FALSE match (no link) |
| `risk.assessed` | broker-a:9092 | risk-service | loan-service | TRUE shared (link) |
| `loan.rejected` | broker-a:9092 | risk-service | (none) | orphan trap |

**DB resources:**
| Schema | Host | DataSource bean | Tables | Type |
|--------|------|-----------------|--------|------|
| `lending` | db-primary:5432 | loan-service default; risk-service `primaryDataSource` | `loans`, `audit_log` | TRUE shared (link) |
| `lending` | db-secondary:5432 | risk-service `reportingDataSource` | `risk_scores`, `audit_log` | FALSE match (same schema, diff host) |

**Trap collisions:**
- Class `LoanDto` in both `com.example.lending.loan.dto` and `com.example.lending.risk.dto`.
- Class `LoanAppliedEvent` copied in both Java repos.
- Spring bean name `auditPublisher` in both repos, different impls.
- Config key `lending.risk.threshold` = `0.7` (loan) vs `0.5` (risk).

---

## Task 1: risk-service — scaffold + compile baseline

**Files:**
- Create: `risk-service/pom.xml`
- Create: `risk-service/src/main/java/com/example/lending/risk/RiskServiceApplication.java`
- Create: `risk-service/src/main/resources/application.yml`

**Produces:** compilable Spring Boot skeleton; `RiskServiceApplication` main class.

- [ ] **Step 1: pom.xml** — Spring Boot 3.2.5 parent; dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-kafka`, `spring-boot-starter-webflux`, `postgresql` (runtime), `grpc-spring-boot-starter` (net.devh 3.1.0.RELEASE) + `grpc-stub`/`grpc-protobuf`, `protobuf-maven-plugin`. Java 17. `groupId com.example.lending`, `artifactId risk-service`.
- [ ] **Step 2: RiskServiceApplication.java** — `@SpringBootApplication` in `com.example.lending.risk`, standard `main`.
- [ ] **Step 3: application.yml** — `server.port: 8082`; datasource placeholders for db-primary + db-secondary (filled Task 4); kafka `broker-a:9092` (filled Task 3); `lending.risk.threshold: 0.5`.
- [ ] **Step 4: Verify compile** — Run `cd risk-service && mvn -q compile`. Expected: BUILD SUCCESS.
- [ ] **Step 5: Commit** — `git init` if needed, `git add -A && git commit -m "chore: scaffold risk-service"`.

## Task 2: risk-service — REST controllers + DTOs + method overloading/overriding

**Files:**
- Create: `.../risk/controller/RiskController.java`, `HealthController.java`, `StatusController.java`
- Create: `.../risk/service/RiskService.java`, `RiskAssessor.java` (interface), `DefaultRiskAssessor.java`
- Create: `.../risk/dto/RiskAssessmentDto.java`, `LoanDto.java`

**Interfaces produced:**
- `RiskController`: `POST /api/v1/risk/assess` (body `LoanDto` → `RiskAssessmentDto`), `GET /api/v1/risk/{id}` → `RiskAssessmentDto`.
- `HealthController`: `GET /api/v1/health`. `StatusController`: `POST /api/v1/status`.
- `RiskService` OVERLOADS: `assessRisk(Long id)`, `assessRisk(Long id, String tier)`, `assessRisk(LoanDto app)` — all return `RiskAssessmentDto`.
- `RiskAssessor` interface `assess(LoanDto)`; `DefaultRiskAssessor implements` it (override) + overrides `toString()`.

- [ ] **Step 1** Write DTOs (`LoanDto`: id, amount, tier, userId; `RiskAssessmentDto`: loanId, score, decision).
- [ ] **Step 2** Write `RiskAssessor` interface + `DefaultRiskAssessor` impl (override `assess`, override `toString`).
- [ ] **Step 3** Write `RiskService` with 3 `assessRisk` overloads; reads `lending.risk.threshold` via `@Value`.
- [ ] **Step 4** Write the 3 controllers with exact paths above.
- [ ] **Step 5: Verify** `mvn -q compile` → SUCCESS. `grep -rn "assessRisk" src` shows 3 overloads.
- [ ] **Step 6: Commit** `git commit -am "feat(risk): rest controllers, dto, overloaded RiskService"`.

## Task 3: risk-service — Kafka (produce risk.assessed + loan.rejected orphan, consume loan.applied)

**Files:**
- Create: `.../risk/kafka/RiskEventProducer.java`, `RiskEventConsumer.java`, `LoanAppliedEvent.java`, `RiskAssessedEvent.java`
- Create: `.../risk/config/KafkaConfig.java`
- Modify: `application.yml` (kafka broker-a)

**Interfaces produced:** consumer `@KafkaListener(topics="loan.applied")` on broker-a; producer publishes `risk.assessed` (broker-a) and `loan.rejected` (broker-a, orphan — no consumer anywhere).

- [ ] **Step 1** `LoanAppliedEvent` POJO (COPY — identical fields to loan-service version; loanId, userId, amount).
- [ ] **Step 2** `KafkaConfig` — `ConsumerFactory`+`ProducerFactory` bootstrap `broker-a:9092`.
- [ ] **Step 3** `RiskEventConsumer` `@KafkaListener(topics="loan.applied", groupId="risk")` → calls `RiskService.assessRisk(LoanDto)`.
- [ ] **Step 4** `RiskEventProducer` — `KafkaTemplate` publish `risk.assessed`; separate method publishes `loan.rejected` (orphan).
- [ ] **Step 5: Verify** `mvn -q compile`; `grep -rn "loan.applied\|risk.assessed\|loan.rejected" src`.
- [ ] **Step 6: Commit** `git commit -am "feat(risk): kafka producer/consumer on broker-a"`.

## Task 4: risk-service — dual DataSource (primary db-primary + reporting db-secondary, both schema lending)

**Files:**
- Create: `.../risk/config/DataSourceConfig.java`
- Create: `.../risk/entity/RiskScore.java`, `AuditLog.java`
- Create: `.../risk/repository/RiskScoreRepository.java`, `AuditLogRepository.java`
- Modify: `application.yml` (two datasources)

**Interfaces produced:** `primaryDataSource` (db-primary:5432, schema lending), `reportingDataSource` (db-secondary:5432, schema lending). `AuditLog` entity maps table `audit_log`. `RiskScore` maps `risk_scores`.

- [ ] **Step 1** `application.yml` two datasource blocks (`spring.datasource.primary.*`, `spring.datasource.reporting.*`) with the exact JDBC URLs incl `currentSchema=lending`.
- [ ] **Step 2** `DataSourceConfig` — two `@Bean @ConfigurationProperties` DataSources + two `EntityManagerFactory`/`TransactionManager` (primary is `@Primary`).
- [ ] **Step 3** Entities `RiskScore` (table `risk_scores`), `AuditLog` (table `audit_log`, schema lending).
- [ ] **Step 4** Repositories for each.
- [ ] **Step 5: Verify** `mvn -q compile`; `grep -rn "db-primary\|db-secondary\|currentSchema=lending" src/main/resources`.
- [ ] **Step 6: Commit** `git commit -am "feat(risk): dual datasource primary+reporting, shared schema name"`.

## Task 5: risk-service — gRPC server

**Files:**
- Create: `risk-service/src/main/proto/risk.proto`
- Create: `.../risk/grpc/RiskGrpcService.java`

**Interfaces produced:** proto `package risk; service RiskService { rpc Assess(AssessRequest) returns (AssessReply); }`. `RiskGrpcService extends RiskServiceImplBase` with `@GrpcService`.

- [ ] **Step 1** `risk.proto` — `AssessRequest{int64 loan_id; double amount;}`, `AssessReply{double score; string decision;}`.
- [ ] **Step 2** `RiskGrpcService` `@GrpcService` overriding `assess`, delegates to `RiskService`.
- [ ] **Step 3: Verify** `mvn -q compile` (protobuf plugin generates stubs) → SUCCESS.
- [ ] **Step 4: Commit** `git commit -am "feat(risk): grpc server"`.

## Task 6: loan-service — scaffold + compile baseline

**Files:**
- Create: `loan-service/pom.xml`, `.../loan/LoanServiceApplication.java`, `loan-service/src/main/resources/application.yml`

Same deps as Task 1 PLUS `spring-cloud-starter-openfeign` (+ spring-cloud dependency management BOM 2023.0.x). `server.port: 8081`. `lending.risk.threshold: 0.7`. `artifactId loan-service`.

- [ ] **Step 1** pom.xml (+ OpenFeign + spring-cloud BOM). **Step 2** `@SpringBootApplication @EnableFeignClients`. **Step 3** application.yml (port 8081, threshold 0.7, broker-a + broker-b placeholders, db-primary datasource, `risk-service.url: http://localhost:8082`).
- [ ] **Step 4: Verify** `cd loan-service && mvn -q compile` → SUCCESS. **Step 5: Commit** `git init`; `git commit -m "chore: scaffold loan-service"`.

## Task 7: loan-service — REST controllers (real targets + same-path traps)

**Files:**
- Create: `.../loan/controller/LoanController.java`, `HealthController.java`, `StatusController.java`
- Create: `.../loan/service/LoanService.java`
- Create: `.../loan/dto/LoanDto.java` (COLLISION with risk-service `LoanDto`), `LoanApplicationRequest.java`

**Interfaces produced:** `POST /api/v1/loans` (real BFF target), `GET /api/v1/loans/{id}` (real risk callback target), `GET /api/v1/health` (same-path trap), `POST /api/v1/status` (same-path trap).

- [ ] **Step 1** DTOs (`LoanDto` — same class name, different package `com.example.lending.loan.dto`).
- [ ] **Step 2** `LoanService.applyLoan(...)`, `getLoan(Long id)`.
- [ ] **Step 3** Controllers with exact paths. **Step 4: Verify** `mvn -q compile`; `grep -rn "/api/v1/health" ../risk-service/src ../loan-service/src` shows both. **Step 5: Commit**.

## Task 8: loan-service — HTTP clients to risk-service (Feign + WebClient + RestTemplate callback)

**Files:**
- Create: `.../loan/client/RiskClient.java` (Feign), `RiskWebClient.java` (WebClient), `LoanCallbackClient.java` (RestTemplate)
- Create: `.../loan/config/HttpClientConfig.java` (WebClient + RestTemplate beans, retry wrapper)

**Interfaces produced:**
- `RiskClient` `@FeignClient(url="${risk-service.url}")` → `POST /api/v1/risk/assess`.
- `RiskWebClient` → `GET /api/v1/risk/{id}` via WebClient.
- `LoanCallbackClient` (RestTemplate) → `GET /api/v1/loans/{id}` (bidirectional edge; models risk→loan callback pattern from loan side).
- `HttpClientConfig`: WebClient bean + RestTemplate bean; one client wrapped in a retry + simple circuit-breaker method.

- [ ] **Step 1** Feign `RiskClient`. **Step 2** `RiskWebClient`. **Step 3** `LoanCallbackClient` + retry/circuit-breaker wrapper. **Step 4** wire into `LoanService.applyLoan` (call Feign assess). **Step 5: Verify** `mvn -q compile`; `grep -rn "api/v1/risk" src`. **Step 6: Commit**.

## Task 9: loan-service — Kafka multi-broker (loan.applied on broker-a TRUE + broker-b FALSE, consume risk.assessed)

**Files:**
- Create: `.../loan/kafka/LoanEventProducer.java`, `RiskAssessedConsumer.java`, `LoanAppliedEvent.java` (COPY)
- Create: `.../loan/config/KafkaConfig.java`
- Modify: `application.yml`

**Interfaces produced:** two `KafkaTemplate` beans — `brokerATemplate` (broker-a:9092), `brokerBTemplate` (broker-b:9092). `LoanEventProducer` publishes `loan.applied` on BOTH templates (broker-a = TRUE shared w/ risk; broker-b = FALSE match). Consumer `@KafkaListener(topics="risk.assessed")` broker-a.

- [ ] **Step 1** `LoanAppliedEvent` COPY. **Step 2** `KafkaConfig` two `ProducerFactory`/`KafkaTemplate` beans (broker-a, broker-b), consumer factory broker-a. **Step 3** `LoanEventProducer.publish(...)` sends on both templates. **Step 4** `RiskAssessedConsumer`. **Step 5: Verify** `mvn -q compile`; `grep -rn "broker-a\|broker-b\|loan.applied\|risk.assessed" src`. **Step 6: Commit**.

## Task 10: loan-service — DB (schema lending on db-primary, tables loans + audit_log)

**Files:**
- Create: `.../loan/entity/Loan.java`, `AuditLog.java`; `.../loan/repository/LoanRepository.java`, `AuditLogRepository.java`
- Modify: `application.yml` (db-primary datasource, currentSchema=lending)

**Interfaces produced:** `Loan` maps table `loans` (schema lending, db-primary — TRUE shared with risk-service which also references it). `AuditLog` maps `audit_log` (name collision with risk's audit_log).

- [ ] **Step 1** application.yml datasource `jdbc:postgresql://db-primary:5432/lending?currentSchema=lending`. **Step 2** entities + repos. **Step 3: Verify** `mvn -q compile`; `grep -rn "db-primary\|table = \"loans\"\|audit_log"`. **Step 4: Commit**.

## Task 11: loan-service — gRPC client + server-side proxy endpoint

**Files:**
- Create: `loan-service/src/main/proto/risk.proto` (COPY of risk-service proto)
- Create: `.../loan/grpc/RiskGrpcClient.java`
- Create: `.../loan/controller/RiskProxyController.java`

**Interfaces produced:** `RiskGrpcClient` `@GrpcClient("risk-service")` blocking stub calls `Assess`. `RiskProxyController` `GET /api/v1/risk-proxy/{id}` → server-side proxy forwarding to risk-service `GET /api/v1/risk/{id}` (via RiskWebClient).

- [ ] **Step 1** copy proto. **Step 2** `RiskGrpcClient`. **Step 3** `RiskProxyController` (uses RiskWebClient from Task 8). **Step 4: Verify** `mvn -q compile`; `grep -rn "risk-proxy"`. **Step 5: Commit**.

## Task 12: loan-service — trap beans + config keys

**Files:**
- Create: `.../loan/config/AuditConfig.java` (bean `auditPublisher`)
- Create matching `.../risk/config/AuditConfig.java` in risk-service (bean `auditPublisher`, different impl)

**Interfaces produced:** Spring bean named `auditPublisher` in BOTH repos, different classes/behavior (bean-name collision trap). Config key `lending.risk.threshold` already differs (0.7 vs 0.5) — verify present in both application.yml.

- [ ] **Step 1** loan-service `AuditConfig` `@Bean("auditPublisher")` returns `KafkaAuditPublisher`. **Step 2** risk-service `AuditConfig` `@Bean("auditPublisher")` returns `DbAuditPublisher`. **Step 3: Verify** both compile; `grep -rn "auditPublisher\|lending.risk.threshold"` across both repos. **Step 4: Commit** in each repo.

## Task 13: portal-bff — scaffold + compile baseline

**Files:**
- Create: `portal-bff/package.json`, `tsconfig.json`, `nest-cli.json`, `src/main.ts`, `src/app.module.ts`

Deps: `@nestjs/core`, `@nestjs/common`, `@nestjs/platform-express`, `@nestjs/graphql`, `@nestjs/apollo`, `@apollo/server`, `graphql`, `axios`, `rxjs`. Dev: `typescript`, `@types/node`.

- [ ] **Step 1** package.json + tsconfig (strict) + nest-cli. **Step 2** `main.ts` bootstrap, `app.module.ts` (imports controllers/services + GraphQLModule code-first). **Step 3: Verify** `cd portal-bff && npm install && npx tsc --noEmit` → no errors. **Step 4: Commit** `git init`; commit.

## Task 14: portal-bff — HTTP clients (axios → loan-service, fetch → risk-service)

**Files:**
- Create: `src/clients/loan.client.ts` (axios), `src/clients/risk.client.ts` (fetch)
- Create: `src/dto/loan.dto.ts`, `src/dto/risk.dto.ts`, `src/dto/dashboard.dto.ts`

**Interfaces produced:** `LoanClient.createLoan(body)` axios → loan-service `POST /api/v1/loans`; `LoanClient.getLoan(id)` → `GET /api/v1/loans/{id}`. `RiskClient.getRisk(id)` fetch → risk-service `GET /api/v1/risk/{id}`.

- [ ] **Step 1** DTOs. **Step 2** `loan.client.ts` axios (baseURL loan-service). **Step 3** `risk.client.ts` native fetch (baseURL risk-service). **Step 4: Verify** `npx tsc --noEmit`; `grep -rn "api/v1/loans\|api/v1/risk" src`. **Step 5: Commit**.

## Task 15: portal-bff — proxy endpoints (pass-through, transforming, aggregating) + same-path health trap

**Files:**
- Create: `src/controllers/proxy.controller.ts`, `src/controllers/portal.controller.ts`, `src/controllers/health.controller.ts`
- Create: `src/services/aggregation.service.ts`

**Interfaces produced:**
- `proxy.controller`: `GET /proxy/loans/:id` — pass-through (returns loan-service response verbatim).
- `portal.controller`: `GET /portal/loans/:id` — transforming (reshape to portal DTO); `GET /portal/dashboard/:userId` — aggregating (LoanClient + RiskClient merged via `aggregation.service`).
- `health.controller`: `GET /api/v1/health` — same-path trap (no link to Java health).
- `aggregation.service` TS OVERLOADS: `merge(loan)`, `merge(loan, risk)` overload signatures + union-typed impl.

- [ ] **Step 1** health.controller (trap). **Step 2** proxy.controller pass-through. **Step 3** aggregation.service with TS overload signatures. **Step 4** portal.controller transform + aggregate. **Step 5: Verify** `npx tsc --noEmit`; `grep -rn "/proxy/loans\|/portal/dashboard\|/api/v1/health" src`. **Step 6: Commit**.

## Task 16: portal-bff — GraphQL resolver

**Files:**
- Create: `src/resolvers/dashboard.resolver.ts`, `src/models/dashboard.model.ts`

**Interfaces produced:** GraphQL code-first `@Resolver` with `@Query dashboard(userId: string)` → resolves via `aggregation.service` (same fan-out to both Java REST services as the aggregating proxy).

- [ ] **Step 1** `dashboard.model.ts` `@ObjectType`. **Step 2** `dashboard.resolver.ts` `@Query`. **Step 3** register in app.module. **Step 4: Verify** `npx tsc --noEmit`; `grep -rn "@Query\|dashboard" src/resolvers`. **Step 5: Commit**.

## Task 17: SCENARIOS.md — ground-truth map

**Files:**
- Create: `SCENARIOS.md` (root: `/Users/divya.lunawat/Development/nisquare/code-review-repos/SCENARIOS.md`)

- [ ] **Step 1** Table for each of 8 scenario groups → exact `repo/path/File.java:line` for every producer/consumer/caller/handler/datasource/bean, marked TRUE-link / FALSE-trap / orphan. Use `grep -n` to capture real line numbers after all code exists.
- [ ] **Step 2: Verify** every row's file:line resolves (`sed -n` spot-check a sample). **Step 3: Commit** (root is not a git repo — this file is meta; leave uncommitted or note in each repo README).

## Task 18: Final verification sweep

- [ ] **Step 1** `cd loan-service && mvn -q compile` → SUCCESS.
- [ ] **Step 2** `cd risk-service && mvn -q compile` → SUCCESS.
- [ ] **Step 3** `cd portal-bff && npx tsc --noEmit` → no errors.
- [ ] **Step 4** Confirm `git log --oneline` non-empty in all 3 repos.
- [ ] **Step 5** Cross-check SCENARIOS.md: every listed file exists.
