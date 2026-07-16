# lending-platform — Multi-Module Traps (Ground Truth)

Maven multi-module repo, one aggregator + 4 modules, base pkg `com.example.lending.platform`.
Tests **intra-repo, cross-module** understanding (vs the cross-*repo* set in `../SCENARIOS.md`).

```
lending-platform/            (aggregator pom)
├── platform-common          (REAL shared library: DTOs, events, util, interface)
├── account-service          (Spring Boot app :8091, broker-a, db-primary)
├── billing-service          (Spring Boot app :8092, broker-b, db-secondary)
└── notification-worker      (Spring Boot app :8093, broker-a consumer)
```

Legend: **TRUE** = real connection tool SHOULD detect · **FALSE** = coincidence tool should NOT link · **ORPHAN** = no counterpart.

## Real shared vs copied (shadow traps)
| Symbol | REAL shared | COPY (shadow, NOT shared) |
|--------|-------------|---------------------------|
| `LoanDto` | `platform-common/.../common/dto/LoanDto.java` | `billing-service/.../billing/dto/LoanDto.java` |
| `MoneyUtil` | `platform-common/.../common/util/MoneyUtil.java` (half-up) | `billing-service/.../billing/util/MoneyUtil.java` (floor) |
| `AccountCreatedEvent` | `platform-common/.../common/event/AccountCreatedEvent.java` (used by account-service on broker-a) | billing-service emits same-named topic on broker-b w/o this type |

`BillingService` imports the billing COPY of `LoanDto` — NOT the shared one. `DefaultFeeCalculator` uses the billing COPY of `MoneyUtil`.

## Cross-module dependency (TRUE)
- account-service + billing-service + notification-worker all depend on `platform-common` (diamond).
- `FeeCalculator` interface declared in `platform-common`, **overridden** in a different module: `billing-service DefaultFeeCalculator` (override across module boundary).

## HTTP (cross-module, TRUE) + same-path (FALSE)
| Edge | Caller | Target |
|------|--------|--------|
| TRUE | billing-service `AccountClient` (Feign) | account-service `GET /api/v1/accounts/{id}` (`AccountController.get`) |
| TRUE | notification-worker `AccountLookupClient` (Feign) | account-service `GET /api/v1/accounts/{id}` |
| FALSE | — | notification-worker `AccountStubController` also serves `GET /api/v1/accounts/{id}` (stub, not the real target) |

**Duplicate `@FeignClient` name** `"account-service"` declared in BOTH billing-service (`AccountClient`) and notification-worker (`AccountLookupClient`) — separate apps, so no runtime clash; a trap for name-based linking.

**Same-path health/status:** `GET /api/v1/health` in all 3 apps; `POST /api/v1/status` in account-service + billing-service. Coincidences, no edges.

## Kafka
| Topic | Broker | Producer | Consumer | Type |
|-------|--------|----------|----------|------|
| `account.created` | broker-a | account-service `AccountEventProducer` | notification-worker `AccountCreatedConsumer` | **TRUE** shared |
| `account.created` | broker-b | billing-service `BillingEventProducer` | (none) | **FALSE** — same topic name, different broker |

## DB
| Schema | Host | Module | Tables | Type |
|--------|------|--------|--------|------|
| `platform` | db-primary:5432 | account-service | `accounts`, `ledger` | reference |
| `platform` | db-secondary:5432 | billing-service | `ledger` | **FALSE** — same schema name, different host |

**Table `ledger`** mapped in both account-service and billing-service (`entity/Ledger.java` each) — same table name, different DB. Name collision, not shared.

## Beans / config
- **Bean name `auditPublisher`** in account-service (`LedgerAuditPublisher`) AND billing-service (`InvoiceAuditPublisher`) — same name, different impl, different module.
- **Config key `platform.fee.rate`** = `0.02` (account-service) vs `0.03` (billing-service).

## Structural
- **Three `@SpringBootApplication`** main classes in one repo: `AccountServiceApplication`, `BillingServiceApplication`, `NotificationWorkerApplication`.
- **Method overloading:** `BillingService.charge(Long)`, `charge(Long,double)`, `charge(LoanDto)`.

## Build
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cd lending-platform && mvn -q compile   # reactor: platform-common first, then 3 apps
```
No services run — static-index fixture.
