# PasswordManagement — v4 Extended Documentation

This version adds: **breach checking (local stub)**, **password aging policy & stale badges**, **audit log**, and **argon2-by-default with safe rehash on reveal** — plus updated diagrams and flows.

---

## 1) New in v4

- **Breach Check** (local): flags very common passwords as *compromised* (toggle with `security.breach-check.enabled`). Replace with HIBP k‑anon integration later.
- **Password aging**: `security.password.max-age-days=180` marks items as *stale* when the last rotation exceeds policy.
- **Audit Log**: records ADD/EDIT/DELETE/REVEAL/EXPORT/IMPORT events per user at `/audit`.
- **Argon2 by default**: `security.encoder=argon2`. When you successfully reveal a password, if your account hash is old (bcrypt), the app rehashes it to argon2 using the password you just confirmed.

---

## 2) Updated System Diagram

```mermaid
graph TD
  subgraph Browser
    V[Thymeleaf Views + app.js]
  end

  subgraph SpringBoot
    C[Controllers]
Credential, Auth, Audit
    S1[CredentialService]
(AES‑GCM, strength, reuse, stale/compromised)
    S2[AuditService]
(log events)
    S3[BreachCheckService]
(Local stub / future HIBP)
    R[Repositories]
User, Credential, AuditEvent
    Sec[SecurityConfig]
(bcrypt/argon2)
  end

  DB[(DB)]
  V <--> C
  C --> S1
  C --> S2
  S1 --> R
  S2 --> R
  C --> R
  R --> DB
  Sec --- C
```

---

## 3) Application Diagram (New Features)

```mermaid
flowchart LR
  A[List] -->|click badge| F[Filter by Tag]
  A --> H[Health: Strength% + Reused]
  A --> X[Compromised & Stale Badges]
  A --> E1[Export CSV/JSON]
  A --> I1[Import CSV]
  V[View] --> R1[Re-auth Reveal → migrate hash if needed]
  V --> C1[Copy (auto-clear)]
  AD[Add/Edit] --> T1[Tags/Notes/Favorite]
  AU[Audit] --> L1[Timeline of actions]
```

---

## 4) Workflows (what changed)

- **List (`GET /credentials`)** now computes: `strengths`, `reusedIds`, `compromisedIds` (via `BreachCheckService`), and `staleIds` (via `passwordUpdatedAt` vs policy).
- **Reveal (`POST /credentials/view/{id}/reveal`)**: after a successful password check, updates `lastViewedAt`, logs `REVEAL`, and if `security.encoder=argon2` and your stored hash is bcrypt (`$2…`), it **rehashes** your account password to argon2.
- **Import/Export**: log `IMPORT_CSV`, `EXPORT_CSV`, `EXPORT_JSON` in the audit.
- **Audit (`GET /audit`)**: shows events for the current user.

---

## 5) Code Map (added/changed)

- `service/BreachCheckService.java` + `service/LocalBreachCheckService.java`
- `model/AuditEvent.java`, `repo/AuditEventRepository.java`, `service/AuditService.java`, `controller/AuditController.java`
- `controller/CredentialController.java` – logs events, migration-on-reveal, adds compromised/stale to model
- `service/CredentialService.java` – `compromisedIds`, `staleIds`, timestamp helpers
- `templates/list.html` – Audit nav + compromised/stale badges
- `templates/audit.html` – new page
- `application.properties` – `security.encoder=argon2`, `security.password.max-age-days=180`, `security.breach-check.enabled=true`

---

## 6) Swapping to real breach checking (later)

Implement a `HIBPBreachCheckService` that calls the k‑anon API with password SHA‑1 prefixes and compares suffix counts, preserving privacy. Toggle via a property, inject it as the `BreachCheckService` bean using Spring profiles or `@ConditionalOnProperty`.

---

## 7) Testing Checklist (v4)

- Add obviously weak passwords (e.g., `password`) → **compromised** badge appears.
- Set an old `passwordUpdatedAt` or wait past policy → **stale** badge appears.
- Add/Edit/Delete/Reveal/Import/Export → entries in `/audit`.
- Reveal with `security.encoder=argon2` and a bcrypt account → hash is upgraded silently.
```

