# PasswordManagement — New Features, System Workflow & Architecture (v2)

This release adds **server-synced Notes**, **Favorites**, **Tags**, a **Password Health** signal, **re-auth to reveal**, **auto‑lock on idle**, and **clipboard auto‑clear**, on top of the previous UI overhaul (dark mode, filter/sort, generator, strength meter, toasts).

---

## What’s new (since previous build)

### Data model
- `Credential.tags` *(String, comma-separated)*
- `Credential.notes` *(@Lob String)* — **server-synced** notes
- `Credential.favorite` *(boolean)*

> JPA auto-migration is enabled via `spring.jpa.hibernate.ddl-auto=update`.

### UX & security
- **List view**: star/unstar favorite, tag badges, health badges (0–100%), reused flag, instant filter & sort.
- **View**: password reveal requires **account password** (re-auth). Copy username/password with **clipboard auto‑clear** (~20s). Notes & tags are displayed.
- **Add/Edit**: tags & notes fields; strength meter + generator + show/hide.
- **Auto‑lock (idle)**: logs out the session after 5 minutes of inactivity.
- **Local notes (optional)**: kept for privacy-conscious users; export/import JSON.

### Export
- CSV now includes **Tags, Favorite, Notes** (password included in plaintext for your own exports).

---

## System architecture (high-level)

```
Browser (Thymeleaf+Bootstrap+JS)
   ├─ /css/app.css       (theme, UI polish)
   └─ /js/app.js         (filter/sort, notes, copy, reveal UI, idle lock)

Spring Boot (MVC + Security + JPA)
   ├─ SecurityConfig              (login/register, protected /credentials/**)
   ├─ Controllers
   │   └─ CredentialController    (CRUD, reveal gate, export, favorites, search)
   ├─ Services
   │   └─ CredentialService       (AES‑GCM encrypt/decrypt, strength, reuse)
   ├─ Repositories
   │   ├─ UserRepository
   │   └─ CredentialRepository
   └─ Entities
       ├─ AppUser                 (username, passwordHash)
       └─ Credential              (site, loginUsername, encryptedPassword, tags, notes, favorite)
Database (H2/MySQL)
```

**Crypto at rest:** Passwords are encrypted using AES‑GCM (random IV per secret) with a master key (`APP_MASTER_KEY`).  
**Auth hashing:** BCrypt as before (upgrade path to Argon2 is straightforward in `SecurityConfig`).

---

## Controller flows

### List `/credentials`
1. Load user → `CredentialRepository.findByOwner`.
2. Compute `strengths` (0–4 → 0–100%) and `reusedIds` via `CredentialService`.
3. Render table with actions (view/edit/delete/toggle favorite).

### Add `/credentials/add` (GET/POST)
- POST: encrypt password → save site, login, **notes**, **tags**, **favorite** → redirect to list.

### View `/credentials/view/{id}` (GET)
- Shows masked password. To reveal, submits to `POST /credentials/view/{id}/reveal` with **account password**.

### Reveal `/credentials/view/{id}/reveal` (POST)
1. Verify account password with `PasswordEncoder.matches`.
2. If ok, decrypt and return `view.html` with `plainPassword` filled.
3. Otherwise show error.

### Edit `/credentials/edit/{id}` (GET/POST)
- Update metadata; if password provided, re-encrypt & save.

### Export `/credentials/export`
- Streams CSV: Site, Username, Password, Tags, Favorite, Notes.

---

## Implementation pointers

- **CredentialService** adds:
  - `passwordStrength(String)` → 0..4
  - `strengthById(List<Credential>)` → map of id → score
  - `reusedPasswords(List<Credential>)` → set of plaintexts used by >1 item
- **CredentialController** handles favorites, search across site/login/tags/notes, and reveal re‑auth.

---

## Next steps (optional)
- Switch **BCrypt → Argon2id** for user auth.
- Browser extension for autofill.
- Shared vaults & item sharing with per-item data keys.
- Breach check integration and password rotation helper.
