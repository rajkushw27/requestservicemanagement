# Counterfoil — a generic maker-checker service

A pluggable four-eyes approval service, built to sit in the middle of an org and be
integrated by whatever systems need a change signed off — not a payments app. Any system
can submit a change for approval; this service works out who must sign it (from the org
chart and a policy), tracks the signatures, enforces segregation of duties, and reports the
outcome. It never interprets the payload, so the same service handles a KYC rating change, a
vendor onboarding, an access grant, a payment limit, or anything else you point at it — see
"Integrating from another system" below for how another system plugs in without a human ever
opening this app's UI.

Java 21 · Spring Boot 3.3 · JPA · H2 (Postgres-ready) backend, with a React (Vite) frontend.
Browser demo only — no iOS/native app in this version.

---

## Run it

Two processes: the API and the SPA.

```bash
# Terminal 1 — backend, http://localhost:8080
cd backend
./mvnw spring-boot:run

# Terminal 2 — frontend, http://localhost:5173
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173** (not 8080 — that's the API). The Vite dev server proxies
`/api/*` to the backend, so there's nothing to configure. Seed data loads automatically on
backend start: 20 people in a five-level org, six approval policies, eight live requests.

H2 is in-memory, so **restarting the backend resets everything**, including who's logged in.
That's normal for a demo — just log back in.

## Logging in

There's no real auth (see CLAUDE.md — this is a demo, and that's the first thing to swap out
for anything real). Each of the 20 seeded employees is a login account:

**User ID = employee ID. Password = `test123` for every single one.**

The login screen has quick-pick buttons for five accounts that walk you through the demo
flow below. The full org:

| User ID | Name | Title | Reports to | Role |
|---|---|---|---|---|
| `emp-01` | Aarav Mehta | Chief Risk Officer | — | ADMIN, APPROVER_L3 |
| `emp-02` | Priya Nair | VP, Payments | emp-01 | APPROVER_L3 |
| `emp-03` | Rohit Sharma | VP, Compliance | emp-01 | APPROVER_L3 |
| `emp-04` | Ananya Rao | Director, Payment Ops | emp-02 | APPROVER_L2 |
| `emp-05` | Vikram Desai | Director, Treasury | emp-02 | APPROVER_L2 |
| `emp-06` | Sneha Iyer | Director, AML | emp-03 | APPROVER_L2 |
| `emp-07` | Karthik Menon | Director, KYC | emp-03 | APPROVER_L2 |
| `emp-08` | Neha Gupta | Manager, Settlements | emp-04 | APPROVER_L1 |
| `emp-09` | Arjun Pillai | Manager, Limits | emp-04 | APPROVER_L1 |
| `emp-10` | Divya Krishnan | Manager, Liquidity | emp-05 | APPROVER_L1 |
| `emp-11` | Sameer Joshi | Manager, Alert Review | emp-06 | APPROVER_L1 |
| `emp-12` | Ritu Bansal | Manager, Onboarding | emp-07 | APPROVER_L1 |
| `emp-13` | Aditya Verma | Settlement Analyst | emp-08 | MAKER |
| `emp-14` | Kavya Reddy | Settlement Analyst | emp-08 | MAKER |
| `emp-15` | Manish Tiwari | Limits Analyst | emp-09 | MAKER |
| `emp-16` | Pooja Shetty | Treasury Analyst | emp-10 | MAKER |
| `emp-17` | Rahul Bose | AML Analyst | emp-11 | MAKER |
| `emp-18` | Ishita Chawla | AML Analyst | emp-11 | MAKER |
| `emp-19` | Nikhil Kulkarni | KYC Officer | emp-12 | MAKER |
| `emp-20` | Tara Fernandes | KYC Officer | emp-12 | MAKER |

## Try the demo in 90 seconds

1. **Log in as `emp-15` / `test123`** (Manish Tiwari, a Limits Analyst — a maker). Go to
   "I submitted" and open `REQ-1001` — raising a customer's daily transfer limit from
   ₹2,00,000 to ₹7,50,000. Notice there's no Approve button: he raised it, so segregation of
   duties keeps him from signing his own request.
2. **Sign out, log in as `emp-09` / `test123`** (Arjun Pillai, his manager). "Waiting on you"
   shows the same request. Open it, add a comment, hit **Approve**. The policy needs two
   signatures, so it stays PENDING and moves to the next person in the chain.
3. **Sign out, log in as `emp-04` / `test123`** (Ananya Rao, Director). Approve it. The
   request closes as APPROVED — check the audit trail at the bottom, which has every step
   with who, when, and what changed.
4. Try it via `emp-15` again and attempt to approve via the API directly (see below) — you
   get a 403. That's `PolicyEngine.assertCanCheck` — the one rule that can't break.

Other things worth poking at:

- **Sequential vs parallel.** `REQ-1001` is sequential (one approver at a time, in order).
  `REQ-1003` uses a parallel AML policy (lands in `emp-08` and `emp-09`'s inboxes) — either
  approver can sign first. Note this policy has `requireManagerChain: false`, so it draws
  checkers from anyone org-wide with the right role/limit, not the maker's own department —
  that's why two Payments managers end up checking an AML case.
- **The high-value bump.** `REQ-1004` is ₹84,00,000, above the `HIGH_VALUE_TRANSFER`
  policy's ₹50,00,000 threshold, so it needs three signatures instead of two — and the
  resolver skips the maker's own director entirely (their ₹50L limit doesn't cover it),
  climbing to VP level and then backfilling a third signer from Compliance.
- **Rejection.** Reject anything — a comment is mandatory, and one rejection closes the
  request regardless of how many approvals it already had.
- **Policies page.** Edit a policy's signature count or SLA live, no redeploy. In-flight
  requests keep the approver chain they were frozen with at submit time — only new
  submissions see the change.
- **New request.** Raise your own from the "+ New request" button — pick a policy, add
  before/after fields, submit, then switch identities to sign it.
- **It's not just payments.** `REQ-1007` (vendor onboarding, `emp-14`'s submission) and
  `REQ-1008` (a system access grant, `emp-20`'s submission) use the exact same engine as the
  financial ones — different `entityType`, different fields in before/after, same
  `ApprovalService` that never looks inside either.

## Deploy it

Both the backend and frontend are ordinary standalone builds — no shared infra assumptions.

### Free hosting on Render (both halves, one dashboard)

`render.yaml` at the repo root is a Render Blueprint: it defines the backend as a free
Docker web service (`maker-checker-api`) and the frontend as a free static site
(`maker-checker-frontend`), and wires them to each other via env vars
(`MAKERCHECKER_CORS_ALLOWED_ORIGINS` on the backend, `VITE_API_BASE` on the frontend, baked
in at build time).

1. Push this repo to GitHub.
2. In the Render dashboard: **New → Blueprint**, pick the repo. Render reads `render.yaml`
   and proposes both services — confirm and deploy.
3. **Name collisions:** Render subdomains (`<name>.onrender.com`) are shared across all
   Render users. If `maker-checker-api` or `maker-checker-frontend` is already taken,
   Render silently assigns a different name, and the cross-wired env vars above will point
   at the wrong URL. After the first deploy, check the actual URLs Render assigned — if
   they don't match what's in `render.yaml`, update `MAKERCHECKER_CORS_ALLOWED_ORIGINS` on
   the backend service and `VITE_API_BASE` on the frontend service in the Render dashboard
   to the real URLs, then trigger a manual redeploy of the frontend (env vars are baked in
   at build time, so it needs a rebuild, not just a restart).
4. Free-tier web services spin down after 15 minutes idle and take ~30-50s to wake on the
   next request — normal for a demo you're driving live, just expect that first load.
   The static site (frontend) doesn't sleep.
5. Data is H2 in-memory, so every backend restart (including the idle spin-down/wake cycle)
   resets the seed data and logs everyone out.

### Other options

**Backend anywhere else:** `backend/Dockerfile` builds a self-contained jar — `docker build
-t maker-checker ./backend && docker run -p 8080:8080 maker-checker` runs it anywhere Docker
runs. For persistence, provision Postgres and set `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` — the driver is already on the
classpath.

**Frontend anywhere else:** `npm run build` in `frontend/` produces a static `dist/` —
deployable to Vercel, Netlify, Nginx, S3, or any static host. Set `VITE_API_BASE` to your
backend's URL before building (it's read at build time, not runtime).

---

## Integrating from another system

The React app is one client of this API — a human-facing one. A workflow engine, an admin
panel, a queue consumer, anything else in your org can call the same endpoints directly,
with no human login involved. Two ways in:

| | Who | How |
|---|---|---|
| **Human, via this UI** | Someone opens Counterfoil and signs in | `Authorization: Bearer <token>` from `POST /api/v1/auth/login` |
| **A system, no human in the loop** | Your service calls the API directly | `X-Api-Key: <key>` + `X-Acting-As: <employeeId>` |

Every endpoint that currently checks `Authorization` accepts either. For the API-key path:

```http
POST /api/v1/approval-requests
X-Api-Key: demo-integration-key-change-me
X-Acting-As: emp-14
```

`X-Api-Key` is checked against `MAKERCHECKER_API_KEY` (one shared demo key — set your own
before deploying anywhere real). `X-Acting-As` is the employee id your system is submitting
or deciding on behalf of; the service trusts that assertion once the key matches — a real
integration would map the caller's own authenticated identity (OAuth client, mTLS cert)
to an employee id server-side rather than trust a client-supplied header (see CLAUDE.md).
Missing `X-Acting-As` with a valid key gets a 400 telling you so; a bad key gets a 401.

**Full API contract, browsable:** once the backend's running, open
**http://localhost:8080/swagger-ui.html** (or `/v3/api-docs` for the raw OpenAPI JSON) —
every endpoint, every request/response shape, both auth schemes, without reading a line of
Java.

**Outcome delivery is already push, not poll** — see "Outcome callback" below. A calling
system submits a request, gets a `202 Accepted` back, and finds out what happened via a
webhook when it's decided. It never needs to ask this service "is it done yet?"

---

## API

Sign in first: `POST /api/v1/auth/login` with `{"username": "emp-15", "password":
"test123"}` returns a bearer token. Send it as `Authorization: Bearer <token>` on everything
else — or use the `X-Api-Key` + `X-Acting-As` pair above if you're a system, not a person.

### Submit for approval

```http
POST /api/v1/approval-requests
Authorization: Bearer <token for the maker>
```

```json
{
  "requestId": "REQ-2001",
  "tenantId": "default",
  "entityType": "CUSTOMER_LIMIT",
  "entityId": "CUST-88213",
  "operation": "UPDATE",
  "summary": "Raise daily transfer limit for Sundar Traders",
  "before": { "dailyLimit": 200000 },
  "after":  { "dailyLimit": 750000 },
  "amount": 750000,
  "policyKey": "PAYMENT_LIMIT_CHANGE",
  "callbackUrl": "https://your-system.example/hooks/approvals"
}
```

`requestId` is the idempotency key — resubmitting the same one for the same tenant returns
the original request rather than creating a duplicate.

### Record a decision

```http
POST /api/v1/approval-requests/{id}/decisions
Authorization: Bearer <token for the checker>
```

```json
{ "decision": "APPROVE", "comments": "Limit is within the customer's turnover band.",
  "expectedVersion": 0 }
```

`decision` is `APPROVE`, `REJECT`, or `REQUEST_CHANGES`. `expectedVersion` is the optimistic
lock — send back the `version` you were shown, and if someone else acted first you get a 409
instead of silently overwriting them. Comments are mandatory unless you are approving.

### Everything else

| | |
|---|---|
| `POST /api/v1/auth/login` | `{username, password}` → `{token, employee}` |
| `GET /api/v1/approval-requests?inboxFor={id}` | what this person can act on right now |
| `GET /api/v1/approval-requests?submittedBy={id}` | what this person raised |
| `GET /api/v1/approval-requests/{id}` | full detail, diff, signatures, audit |
| `GET /api/v1/approval-requests/{id}/audit` | audit trail alone |
| `POST /api/v1/approval-requests/{id}/recall` | maker withdraws |
| `GET /api/v1/people` | the demo org |
| `GET /api/v1/policies` · `PUT /api/v1/policies/{key}` | read and edit rules at runtime |
| `GET /swagger-ui.html` · `GET /v3/api-docs` | browsable / machine-readable API contract |

### Outcome callback

When a request reaches a terminal state, an event is written to a transactional outbox and
POSTed to `callbackUrl`:

```json
{ "approvalRequestId": "…", "requestId": "REQ-2001", "entityType": "CUSTOMER_LIMIT",
  "entityId": "CUST-88213", "operation": "UPDATE", "status": "APPROVED",
  "decidedBy": "emp-04", "decidedAt": "2026-07-23T09:14:22Z", "detail": "…" }
```

Headers carry `X-Event-Id` and `X-Event-Type`. Delivery is at-least-once — dedupe on the
event id. **Your system applies the change**; this service only tells you it was authorised.

---

## Policies

A policy is what makes the service reusable. It is a row in the database, editable at runtime.

| Field | What it does |
|---|---|
| `minApprovals` | base number of signatures |
| `amountThreshold` | above this amount, require one more signature |
| `mode` | `SEQUENTIAL` (in order) or `PARALLEL` (any order) |
| `allowedApproverRoles` | which roles may sign |
| `requireManagerChain` | draw approvers from the maker's reporting line |
| `enforceApprovalLimit` | approver's own limit must cover the amount |
| `excludeMaker` | segregation of duties — always on in practice |
| `slaMinutes` + `onTimeout` | `EXPIRE`, `ESCALATE`, or `AUTO_REJECT` |
| `allowSelfRecall` | may the maker withdraw before a decision |

Seeded policies: `PAYMENT_LIMIT_CHANGE` (2, sequential, escalates after 48h),
`KYC_RISK_RATING` (1, expires after 24h), `AML_CASE_CLOSURE` (2, parallel, auto-rejects
after 12h), `HIGH_VALUE_TRANSFER` (2, or 3 above ₹50,00,000, escalates after 4h),
`VENDOR_ONBOARDING` (2, sequential, escalates after 24h), `ACCESS_GRANT` (1, expires after 8h).
The first four happen to be financial because that's a recognizable demo domain — the last
two are there specifically to show the policy shape doesn't care.

## The org

One CRO → two VPs → four Directors → five Managers → eight analysts and officers. Approval
limits run from ₹10,00,000 at manager level to ₹10,00,00,000 at the top, so the manager-chain
resolver has to walk far enough up the tree to find someone who can actually authorise the
amount. That is the bit worth showing in a demo.

Full hierarchy diagram, with reporting lines and approval limits: **[docs/org-chart.md](docs/org-chart.md)**.

## What is deliberately missing

Real auth (see CLAUDE.md — the login is a demo convenience, not security), no tests, no
migrations, H2 in memory, and no native/iOS app. This is a demo, not a product. `CLAUDE.md`
lists what to build next and in what order.
