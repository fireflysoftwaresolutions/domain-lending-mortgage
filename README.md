<a href="https://fireflysoftware.io/openplatform/">
<picture>
  <source media="(max-width: 640px) and (prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-dark.svg">
  <source media="(max-width: 640px)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-light.svg">
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-dark.svg">
  <img src="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-light.svg" alt="Firefly OpenPlatform - Domain Lending - Mortgage" width="100%">
</picture>
</a>

# Firefly OpenPlatform - Domain Lending - Mortgage

Developed by the Firefly Software Foundation. [dev@fireflysoftware.io](mailto:dev@fireflysoftware.io)

[Website](https://fireflysoftware.io/) · [OpenPlatform](https://fireflysoftware.io/openplatform/) · [Module catalogue](https://fireflysoftware.io/openplatform/modules/)

Domain layer microservice for mortgage agreements within the Firefly platform. This service orchestrates create, update, status change, close, and query against `core-lending-mortgage` with compensating saga steps.

Repository: [https://github.com/fireflysoftwaresolutions/domain-lending-mortgage](https://github.com/fireflysoftwaresolutions/domain-lending-mortgage)

---

## Overview

`domain-lending-mortgage` sits between experience services and `core-lending-mortgage`. Write operations run through the FireflyFramework saga engine. Queries use the CQRS query bus. The domain depends on `core-lending-mortgages-sdk` only. It does not import another core's internal packages. Identifiers that point at parties, contracts, or products stay UUIDs owned by those cores.

It creates, updates, changes status, and closes mortgage agreements through `core-lending-mortgage`. The mortgage core exposes the agreement as its write API. Party, contract, and product links stay UUIDs on the agreement.

### Key features

- **Create** -- register a mortgage agreement through `core-lending-mortgage`, with compensation that deletes the core write
- **Update** -- read the current mortgage agreement and write the new field values through `update` on `core-lending-mortgage`. Status is left unchanged. Compensation writes the previous record back.
- **Status change** -- read the current mortgage agreement and update its status through `core-lending-mortgage`. The mortgage core has no status history resource, so the status saga does not append one.
- **Close** -- Close and cancel run the status saga. Remove deletes the agreement, which the core allows for every status. **Close** writes `CLOSED` when the core lifecycle allows that move. **Cancel** writes `CANCELLED` when the core lifecycle allows that move.
- **Query** -- read one mortgage agreement by identifier
- **Compensation** -- a failed later step asks the core to undo the work already accepted
- **CQRS** -- commands and queries travel on separate buses
- **Reactive I/O** -- Spring WebFlux with virtual threads enabled

## Architecture

### Module structure

| Module | Purpose |
|--------|---------|
| `domain-lending-mortgage-core` | Saga workflows, commands, handlers, and the application service |
| `domain-lending-mortgage-interfaces` | Request contracts for a status change and a closing reason |
| `domain-lending-mortgage-infra` | SDK client factory and downstream base-path configuration |
| `domain-lending-mortgage-web` | Spring Boot application, REST API, actuator, and OpenAPI |
| `domain-lending-mortgage-sdk` | Java client generated from the OpenAPI document |

### Dependency flow

```
web --> interfaces --> core --> infra --> core-lending-mortgages-sdk
sdk (generated from openapi.yml)
```

### Saga map

| Saga | Core calls | Compensation |
|------|------------|--------------|
| `RegisterMortgageAgreementSaga` | `create` on `MortgageAgreementApi` | Deletes the record |
| `UpdateMortgageAgreementSaga` | Reads the current record, then writes field changes through `update` on `MortgageAgreementApi`. Status is left unchanged. | Writes the previous record back through `update` |
| `UpdateMortgageAgreementStatusSaga` | Reads the current record, updates its status through `update` | Writes the previous status back through the core |
| Query | `getById` on `MortgageAgreementApi` | None. A read has nothing to undo. |

Status values accepted by the core: `DRAFT`, `ACTIVE`, `SUSPENDED`, `CLOSED`, `CANCELLED`.

## Setup

### Prerequisites

- Java 25
- Apache Maven 3.9+
- `core-lending-mortgages-sdk` 26.10.4
- Apache Kafka on `localhost:9092` when event publishing is left at the default

### Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_ADDRESS` | `localhost` | Bind address |
| `SERVER_PORT` | `8045` | HTTP port |
| `ENDPOINT_CORE_LENDING_MORTGAGES` | `http://localhost:8080` | Base URL of `core-lending-mortgage` |

### Build and test

```bash
mvn clean verify
```

### Run

```bash
ENDPOINT_CORE_LENDING_MORTGAGES=http://localhost:8080 mvn spring-boot:run -pl domain-lending-mortgage-web
```

Swagger UI (non-production): [http://localhost:8045/swagger-ui.html](http://localhost:8045/swagger-ui.html)

## API

| Method | Path | Behavior |
|--------|------|----------|
| `POST` | `/api/v1/mortgage-agreements` | Runs `RegisterMortgageAgreementSaga` |
| `GET` | `/api/v1/mortgage-agreements/{mortgageAgreementId}` | Queries the core |
| `PUT` | `/api/v1/mortgage-agreements/{mortgageAgreementId}/status` | Runs `UpdateMortgageAgreementStatusSaga` |
| `PUT` | `/api/v1/mortgage-agreements/{mortgageAgreementId}` | Runs `UpdateMortgageAgreementSaga` |
| `POST` | `/api/v1/mortgage-agreements/{mortgageAgreementId}/close` | Close mortgage agreement. Runs the status saga when `core-lending-mortgage` allows `CLOSED`. |
| `POST` | `/api/v1/mortgage-agreements/{mortgageAgreementId}/cancel` | Cancel mortgage agreement. Runs the status saga when `core-lending-mortgage` allows `CANCELLED`. |
| `DELETE` | `/api/v1/mortgage-agreements/{mortgageAgreementId}` | Deletes the record through `core-lending-mortgage` when that core allows removal |

The status request body is `status` plus an optional `statusReason`.

Close writes CLOSED and cancel writes CANCELLED. The mortgage core does not restrict which status can follow another. Remove deletes any agreement because the core delete has no status guard.
A closing command body is an optional `statusReason`.
