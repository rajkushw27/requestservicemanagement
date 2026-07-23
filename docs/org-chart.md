# Org chart — the demo org

The 20 people `DemoDataSeeder` creates on every backend start. One CRO → two VPs → four
Directors → five Managers → eight analysts/officers. This is the source approval chains are
resolved from: `PolicyEngine.resolveApprovers` climbs this tree from a maker upward until it
collects enough eligible signers, then backfills from the wider org if the tree runs out.

If you change the org in `DemoDataSeeder.java`, update this diagram to match — it's not
generated from the code, it's a hand-drawn mirror of it.

```mermaid
flowchart TD
    classDef cro fill:#3454d1,color:#ffffff,stroke:#28409e,stroke-width:1px
    classDef vp fill:#6c8cff,color:#0b1030,stroke:#3454d1,stroke-width:1px
    classDef director fill:#b06a00,color:#ffffff,stroke:#8a5400,stroke-width:1px
    classDef manager fill:#1a6f8a,color:#ffffff,stroke:#125267,stroke-width:1px
    classDef maker fill:#eef0f6,color:#12141c,stroke:#c7ccdb,stroke-width:1px

    emp01["<b>emp-01 · Aarav Mehta</b><br/>Chief Risk Officer<br/>ADMIN, APPROVER_L3 · limit ₹10,00,00,000"]:::cro

    emp02["<b>emp-02 · Priya Nair</b><br/>VP, Payments<br/>APPROVER_L3 · limit ₹2,50,00,000"]:::vp
    emp03["<b>emp-03 · Rohit Sharma</b><br/>VP, Compliance<br/>APPROVER_L3 · limit ₹2,50,00,000"]:::vp

    emp04["<b>emp-04 · Ananya Rao</b><br/>Director, Payment Ops<br/>APPROVER_L2 · limit ₹50,00,000"]:::director
    emp05["<b>emp-05 · Vikram Desai</b><br/>Director, Treasury<br/>APPROVER_L2 · limit ₹50,00,000"]:::director
    emp06["<b>emp-06 · Sneha Iyer</b><br/>Director, AML<br/>APPROVER_L2 · limit ₹50,00,000"]:::director
    emp07["<b>emp-07 · Karthik Menon</b><br/>Director, KYC<br/>APPROVER_L2 · limit ₹50,00,000"]:::director

    emp08["<b>emp-08 · Neha Gupta</b><br/>Manager, Settlements<br/>APPROVER_L1 · limit ₹10,00,000"]:::manager
    emp09["<b>emp-09 · Arjun Pillai</b><br/>Manager, Limits<br/>APPROVER_L1 · limit ₹10,00,000"]:::manager
    emp10["<b>emp-10 · Divya Krishnan</b><br/>Manager, Liquidity<br/>APPROVER_L1 · limit ₹10,00,000"]:::manager
    emp11["<b>emp-11 · Sameer Joshi</b><br/>Manager, Alert Review<br/>APPROVER_L1 · limit ₹10,00,000"]:::manager
    emp12["<b>emp-12 · Ritu Bansal</b><br/>Manager, Onboarding<br/>APPROVER_L1 · limit ₹10,00,000"]:::manager

    emp13["<b>emp-13 · Aditya Verma</b><br/>Settlement Analyst<br/>MAKER"]:::maker
    emp14["<b>emp-14 · Kavya Reddy</b><br/>Settlement Analyst<br/>MAKER"]:::maker
    emp15["<b>emp-15 · Manish Tiwari</b><br/>Limits Analyst<br/>MAKER"]:::maker
    emp16["<b>emp-16 · Pooja Shetty</b><br/>Treasury Analyst<br/>MAKER"]:::maker
    emp17["<b>emp-17 · Rahul Bose</b><br/>AML Analyst<br/>MAKER"]:::maker
    emp18["<b>emp-18 · Ishita Chawla</b><br/>AML Analyst<br/>MAKER"]:::maker
    emp19["<b>emp-19 · Nikhil Kulkarni</b><br/>KYC Officer<br/>MAKER"]:::maker
    emp20["<b>emp-20 · Tara Fernandes</b><br/>KYC Officer<br/>MAKER"]:::maker

    emp01 --> emp02
    emp01 --> emp03

    emp02 --> emp04
    emp02 --> emp05
    emp03 --> emp06
    emp03 --> emp07

    emp04 --> emp08
    emp04 --> emp09
    emp05 --> emp10
    emp06 --> emp11
    emp07 --> emp12

    emp08 --> emp13
    emp08 --> emp14
    emp09 --> emp15
    emp10 --> emp16
    emp11 --> emp17
    emp11 --> emp18
    emp12 --> emp19
    emp12 --> emp20
```

## Reading it

- **Color = approval limit tier**, not department. Notice Treasury (`emp-05`, `emp-10`,
  `emp-16`) sits organizationally under the VP of *Payments* (`emp-02`), not under a separate
  VP — that's deliberate seed data, not a bug, and it's why the `HIGH_VALUE_TRANSFER` chain
  in the demo script climbs through Payments leadership for a Treasury analyst's request.
- **The tree is only where chain resolution *starts*.** Policies with
  `requireManagerChain: false` (like `AML_CASE_CLOSURE`) ignore this shape entirely and pick
  checkers org-wide by role and approval limit — see `PolicyEngine.resolveApprovers` and the
  README's "Sequential vs parallel" note for why `REQ-1003` ends up with two *Payments*
  managers checking an AML case.
- **A chain can skip rungs.** `PolicyEngine` only adds a candidate if their own
  `approvalLimit` covers the request amount. A director whose limit is too low is climbed
  past, not stopped at — see the "high-value chain skips a rung" beat in the demo script.

Every account here logs in with **user ID = employee ID, password `test123`** — see the
repo README for the full table and the demo script for who to use when.
