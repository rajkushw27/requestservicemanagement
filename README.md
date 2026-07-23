# Counterfoil — a generic maker-checker service

A pluggable four-eyes approval service. Any system can submit a change for approval; this
service works out who must sign it, tracks the signatures, enforces segregation of duties,
and reports the outcome. It never interprets the payload, so the same service handles a KYC
rating change, a payment limit, or anything else you point at it.

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
backend start: 20 people in a five-level org, four approval policies, six live requests.

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
  `REQ-1003` (`emp-11`'s inbox) uses a parallel AML policy — either of two approvers can sign
  first.
- **The high-value bump.** `REQ-1004` is ₹84,00,000, above the `HIGH_VALUE_TRANSFER`
  policy's ₹50,00,000 threshold, so it needs three signatures instead of two.
- **Rejection.** Reject anything — a comment is mandatory, and one rejection closes the
  request regardless of how many approvals it already had.
- **Policies page.** Edit a policy's signature count or SLA live, no redeploy. In-flight
  requests keep the approver chain they were frozen with at submit time — only new
  submissions see the change.
- **New request.** Raise your own from the "+ New request" button — pick a policy, add
  before/after fields, submit, then switch identities to sign it.

## Deploy it

Both the backend and frontend are ordinary standalone builds — no shared infra assumptions.

**Backend:** `backend/Dockerfile` builds a self-contained jar (H2 in-memory, nothing to
provision). `docker build -t maker-checker ./backend && docker run -p 8080:8080
maker-checker`. For persistence, provision Postgres and set `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` — the driver is already on the
classpath.

**Frontend:** `npm run build` in `frontend/` produces a static `dist/` you can serve from
any static host (Vercel, Netlify, Nginx, S3). Point it at your deployed backend by setting
the dev proxy target or, for production, fronting both behind the same reverse proxy so
`/api` reaches the backend.

---

## API

Sign in first: `POST /api/v1/auth/login` with `{"username": "emp-15", "password":
"test123"}` returns a bearer token. Send it as `Authorization: Bearer <token>` on everything
else.

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
after 12h), `HIGH_VALUE_TRANSFER` (2, or 3 above ₹50,00,000, escalates after 4h).

## The org

One CRO → two VPs → four Directors → five Managers → eight analysts and officers. Approval
limits run from ₹10,00,000 at manager level to ₹10,00,00,000 at the top, so the manager-chain
resolver has to walk far enough up the tree to find someone who can actually authorise the
amount. That is the bit worth showing in a demo.

## What is deliberately missing

Real auth (see CLAUDE.md — the login is a demo convenience, not security), no tests, no
migrations, H2 in memory, and no native/iOS app. This is a demo, not a product. `CLAUDE.md`
lists what to build next and in what order.
