# ms-audit

Microserviço responsável pelo log de auditoria imutável (append-only) da plataforma **Switchboard**. Consome eventos Kafka de todos os serviços e expõe uma API de consulta com filtros. Parte da [arquitetura de microsserviços](https://github.com/CassioCintra/platform-ops/blob/main/ARCHITECTURE.md).

## Stack

| Componente  | Tecnologia                            |
|-------------|---------------------------------------|
| Runtime     | Java 25                               |
| Framework   | Spring Boot 4.0.6                     |
| Banco       | PostgreSQL 16 (JSONB para payload)    |
| Event Bus   | Kafka 4.0 (KRaft) — consumer only     |
| Auth        | ms-auth (Spring Authorization Server) |
| Migrations  | Flyway                                |
| Testes      | JUnit 5 + Testcontainers              |

## Rodando localmente

### Pré-requisitos

- Java 25
- Docker

### 1. Subir a infraestrutura

```bash
# No repositório platform-ops
docker compose up -d
```

Sobe PostgreSQL (5432), Kafka (9094), ms-auth (9000) e os demais serviços.

### 2. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

A API ficará disponível em:

```
http://localhost:8084/audit/v1
```

Swagger UI disponível em:
- **Direto:** `http://localhost:8084/audit/v1/swagger-ui.html`
- **Via gateway:** `http://localhost:8090/swagger-ui.html` (seletor com todos os serviços)

## API

### Base URL

```
http://localhost:8084/audit/v1
```

Todas as rotas exigem JWT válido no header `Authorization: Bearer <token>`.

### Endpoints

| Método | Rota            | Descrição                              |
|--------|-----------------|----------------------------------------|
| `GET`  | `/events`       | Lista eventos com filtros + paginação  |
| `GET`  | `/events/{id}`  | Retorna um evento pelo UUID            |

### Parâmetros de filtro — `GET /events`

| Parâmetro    | Tipo         | Descrição                                       |
|--------------|--------------|-------------------------------------------------|
| `source`     | `AuditSource`| `FLAG_EVENT`, `USER_INVITED`, `TOKEN_REVOKED`   |
| `action`     | `String`     | ex: `CREATED`, `DELETED`, `INVITED`, `REVOKED`  |
| `actorId`    | `String`     | ID ou email de quem realizou a ação             |
| `resourceId` | `String`     | Identificador do recurso afetado                |
| `from`       | `Instant`    | Início do período (ISO-8601)                    |
| `to`         | `Instant`    | Fim do período (ISO-8601)                       |
| `page`       | `int`        | Número da página (default: 0)                   |
| `size`       | `int`        | Tamanho da página (default: 20)                 |

### Exemplos

**Listar eventos de uma flag**

```
GET /audit/v1/events?source=FLAG_EVENT&resourceId=checkout_v2&size=10
```

**Listar convites de um workspace em um período**

```
GET /audit/v1/events?source=USER_INVITED&from=2026-01-01T00:00:00Z&to=2026-06-01T00:00:00Z
```

**Buscar evento por ID**

```
GET /audit/v1/events/550e8400-e29b-41d4-a716-446655440000
```

**Resposta**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "source": "FLAG_EVENT",
  "action": "CREATED",
  "actorId": "user-123",
  "resourceId": "checkout_v2",
  "payload": {
    "flagName": "checkout_v2",
    "serviceName": "billing",
    "type": "ROLLOUT",
    "rollout": 30,
    "enabled": false
  },
  "occurredAt": "2026-06-07T10:30:00Z"
}
```

## Eventos Kafka consumidos

| Tópico          | Source           | `action` gravado          |
|-----------------|------------------|---------------------------|
| `flag.events`   | `FLAG_EVENT`     | `CREATED`, `UPDATED`, `TOGGLED`, `DELETED` |
| `user.invited`  | `USER_INVITED`   | `INVITED`                 |
| `token.revoked` | `TOKEN_REVOKED`  | `REVOKED`                 |

O campo `payload` armazena o evento completo em JSONB para rastreabilidade futura.

## Banco de dados

### Tabela `audit_events`

| Coluna        | Tipo          | Descrição                            |
|---------------|---------------|--------------------------------------|
| `id`          | `UUID`        | PK gerada pelo banco                 |
| `source`      | `VARCHAR(32)` | Origem do evento (`AuditSource`)     |
| `action`      | `VARCHAR(32)` | Ação realizada                       |
| `actor_id`    | `VARCHAR(255)`| Quem realizou a ação                 |
| `resource_id` | `VARCHAR(255)`| Recurso afetado                      |
| `payload`     | `JSONB`       | Evento completo                      |
| `occurred_at` | `TIMESTAMPTZ` | Quando ocorreu (índice DESC)         |

Índices em `source`, `action`, `actor_id` e `occurred_at DESC` para suportar os filtros da API.

## Arquitetura

Arquitetura hexagonal (ports & adapters):

```
src/main/java/io/github/cassio/ms_audit/
├── domain/
│   ├── audit/          # AuditEvent, AuditSource
│   └── exception/      # AuditEventNotFoundException
├── application/
│   ├── port/in/        # SaveAuditEventUseCase, QueryAuditEventsUseCase
│   ├── port/out/       # AuditEventRepository
│   └── service/        # AuditEventService
└── adapter/
    ├── in/
    │   ├── messaging/  # FlagEventConsumer, UserInvitedEventConsumer, TokenRevokedEventConsumer
    │   └── web/        # AuditEventController, SecurityConfig, CorrelatorFilter
    └── out/
        └── persistence/ # AuditEventEntity, AuditEventJpaRepository, AuditEventPersistenceAdapter
```

## Testes

```bash
# Unitários (sem Docker)
./mvnw test -Dtest="AuditEventServiceTest,AuditEventControllerTest,FlagEventConsumerTest,UserInvitedEventConsumerTest,TokenRevokedEventConsumerTest"

# Integração (requer Docker para Testcontainers)
./mvnw test -Dtest="AuditEventPersistenceAdapterTest,MsAuditApplicationTests"

# Gerar spec OpenAPI (requer Docker — usado também pela pipeline de CI)
./mvnw test -Dtest=OpenApiSpecGeneratorTest

# Todos
./mvnw verify
```

| Tipo               | Ferramentas                                   | Exemplo                          |
|--------------------|-----------------------------------------------|----------------------------------|
| Web slice          | `@WebMvcTest` + MockMvc                       | `AuditEventControllerTest`       |
| Serviço (unit)     | JUnit 5 + Mockito                             | `AuditEventServiceTest`          |
| Consumer (unit)    | Mockito + `@Spy ObjectMapper`                 | `FlagEventConsumerTest`          |
| Persistência (int) | `@DataJpaTest` + Testcontainers (PostgreSQL)  | `AuditEventPersistenceAdapterTest` |
| Smoke test         | `@SpringBootTest` + Testcontainers            | `MsAuditApplicationTests`        |
| OpenAPI spec       | `@SpringBootTest` + MockMvc + Testcontainers  | `OpenApiSpecGeneratorTest`       |

## Contexto no ecossistema Switchboard

```
Fase 1 — ms-feature-flags          ✅ concluída
Fase 2 — ms-workspace-management   ✅ concluída
Fase 3 — ms-auth                   ✅ concluída
Fase 4 — Spring Cloud Gateway      ✅ concluída
Fase 4.1 — OpenAPI specs + pipelines  ✅ concluída
Fase 4.2 — Dockerfiles             ✅ concluída
Fase 5 — ms-audit                  ✅ este serviço
Fase 6 — Workspace-scoping         ⏳
Fase 7 — Adequação LGPD            ⏳
```

Documentação completa da plataforma: [`platform-ops/docs/architecture`](https://github.com/CassioCintra/platform-ops/blob/main/docs/architecture/README.md)
