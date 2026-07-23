# CLAUDE.md

Working notes for Claude Code in this repo. Read this before changing anything.

## What this is

A **generic, entity-agnostic maker-checker (four-eyes) approval service**. Any workflow
system can POST a change for approval; this service decides who must sign it, tracks the
signatures, and reports the outcome. It deliberately does **not** know what is being
approved and does **not** execute the approved change — it records the decision and fires
a callback. The calling system applies the change.

Stack: Java 21, Spring Boot 3.3, Spring Data JPA, H2 in-memory (Postgres-ready), React (Vite) SPA
frontend. iOS/native is explicitly out of scope for now — this is a browser demo.

## The one rule that must never break

**The maker can never be their own checker.** Segregation of duties is the entire point of
the service. `PolicyEngine.assertCanCheck` enforces it. If you touch that method, add a test.

## Architecture decisions already made — do not relitigate without asking

- **No workflow engine.** Spring StateMachine is in maintenance mode; Camunda 7 CE is EOL;
  Camunda 8 needs a paid licence and a Zeebe cluster. A 6-state machine does not justify any
  of them. The state machine is a status enum plus guarded transitions in `ApprovalService`.
- **Decision-only.** The service returns a decision; it does not apply the change. This is
  what keeps it reusable across domains. If someone asks for execution, that is a new
  `ExecutionAdapter` SPI, not a change to `ApprovalService`.
- **Outcome delivery via transactional outbox.** The outbox row is written in the same
  transaction as the state change. `OutboxRelay` POSTs it to the caller's webhook.
  At-least-once — consumers dedupe on `X-Event-Id`. Swapping to Kafka means replacing the
  `RestClient` call in `OutboxRelay` and nothing else.
- **Approver chain is frozen at submit time** onto `ApprovalRequest.approverChain`. Editing a
  policy later must never change who was supposed to approve an in-flight request.
- **Audit rows are append-only.** Never add an update or delete path to `AuditEvent`.

## Layout

```
backend/src/main/java/com/rajani/makerchecker/
  domain/     JPA entities. ApprovalRequest is the aggregate root.
  repo/       Spring Data interfaces, one per file (nested interfaces break scanning).
  service/    ApprovalService = state machine. PolicyEngine = who may approve.
              SlaSweeper = deadline handling. OutboxRelay = outcome delivery.
              TokenAuth = demo session store (see Identity below).
  web/        REST controllers, DTOs (records), ViewMapper, exception handler,
              AuthController + AuthSupport for login/session resolution.
  config/     DemoDataSeeder — 20-person org, 4 policies, 6 live requests. WebConfig — CORS.
frontend/     React (Vite) SPA. Plain CSS, no component library, no build step surprises.
```

## Identity

Each of the 20 seeded employees is a login account: **user ID = employee ID (e.g. `emp-15`),
password `test123` for all of them** (see README for the full list / quick picks on the login
screen). `POST /api/v1/auth/login` checks the password and issues an opaque bearer token;
`TokenAuth` holds an in-memory `token -> employeeId` map that is gone on restart, same as
everything else in H2. This is still not real auth — there's no password hashing, no
expiry, no refresh — it exists so the demo has a login screen and a session per person
instead of a raw `X-User-Id` header. Replacing it with a real JWT filter is still the first
thing to do before this touches anything real (see below). Controllers resolve the acting
employee from the bearer token via `AuthSupport.currentUser(authorizationHeader)`; do not
scatter identity lookups through the service layer.

## Running

```bash
cd backend && ./mvnw spring-boot:run     # http://localhost:8080
cd frontend && npm install && npm run dev # http://localhost:5173, proxies /api to 8080
```

Seed data loads on first start. H2 is in-memory, so a restart resets everything — which is
usually what you want for a demo, but it also means every login session resets on restart.
`/h2-console` is enabled (JDBC URL `jdbc:h2:mem:makerchecker`).

## Things worth building next, roughly in order

1. **Tests.** There are none yet. Start with `PolicyEngineTest` (maker-exclusion, sequential
   turn-taking, the high-value approver bump) then `ApprovalServiceTest` (idempotent submit,
   double-decision rejection, SLA transitions).
2. **Real auth.** Replace `TokenAuth`'s in-memory map and plaintext password check with a
   real identity provider (JWT filter, hashed passwords, expiry) — see Identity above.
3. **Postgres + Flyway.** `ddl-auto: update` is fine for a demo and wrong for anything else.
4. **ShedLock** on `SlaSweeper` and `OutboxRelay` before running more than one replica.
5. **Delegation / out-of-office**, so an approver can hand their queue to someone else —
   with the delegation itself recorded in the audit trail.
6. **Live updates** to the frontend (polling or push) so approvers do not have to refresh.

## Conventions

- Records for DTOs, classes for entities.
- No Lombok.
- User-facing strings are plain and active: "Approve", "Reject", "Withdraw request".
  Errors say what happened and what to do: "This request changed while you were reviewing it.
  Reload and try again."
- Keep `ApprovalService` free of domain knowledge about payments, KYC, or anything else.
  If a change requires the service to interpret `payloadAfter`, the design has gone wrong.
