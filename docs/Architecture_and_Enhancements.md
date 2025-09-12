# PasswordManagement — System Architecture & UX Enhancements

This document summarizes the **improvements**, **component workflows**, and **system architecture** of the PasswordManagement application.

---

## 1) What we improved

### UX & UI
- **Bootstrap 5** across all pages, with a consistent layout.
- **Dark/Light theme toggle** (persists in `localStorage`).
- **List page upgrades**: inline **filter**, **click-to-sort** headers, and a **Notes** button per row.
- **View page upgrades**: **Show/Hide** password, **Copy** username/password, Notes panel.
- **Add/Edit forms**: **password strength meter**, **show/hide** toggle, and **secure generator**.
- **Toasts** for quick feedback (copied, saved, generated).
- **Delete confirmation** to avoid accidental removal.

### Notes Feature
- Per-credential **private notes** stored locally in the browser (`localStorage`), keyed as `pm.notes:<credentialId>`.
- **Export/Import** notes (JSON) from the list page.
- Notes are not sent to the server by design (privacy first).

### Files added/modified
- **Static assets**
  - `src/main/resources/static/css/app.css`
  - `src/main/resources/static/js/app.js`
- **Templates**
  - `templates/list.html` — filter/sort, Notes button, asset includes
  - `templates/view.html` — password tools + Notes panel
  - `templates/add.html`, `templates/edit.html` — strength meter + generator + toggles
  - `templates/login.html`, `templates/register.html`, `templates/search.html` — style + includes
- **Documentation**
  - `docs/Architecture_and_Enhancements.md` (this file)

> If you want **server-side, synced notes**, add a `notes` column to the `Credential` entity and surface it in the controller/templates. See “Future extensions” at the end.

---

## 2) System architecture (high level)

```
+------------------+           +----------------------+           +------------------+
|  Browser (UI)    |  HTTP     |  Spring MVC          |  JPA/HQL  |  Database        |
|  Thymeleaf views | <-------> |  Controllers/Services| <-------> |  (H2/MySQL)      |
+------------------+           +----------------------+           +------------------+
          ^                                ^                                   |
          |  /css/app.css, /js/app.js      |                                   |
          +------------------------------- static ------------------------------+
```

- **Controllers** (`AuthController`, `CredentialController`) handle web routes and bind form data.
- **Services** (`CredentialService`, `CustomUserDetailsService`) encapsulate business logic and security integration.
- **Repositories** (`UserRepository`, `CredentialRepository`) handle persistence.
- **Models** (`AppUser`, `Credential`) map to tables (via JPA/Hibernate).
- **Security** (`SecurityConfig`) defines login, session, and URL access rules.
- **Views** (Thymeleaf templates) render the UI. Our JS enhances them for better UX.

---

## 3) Component-by-component workflow

### 3.1 AuthController
- **GET `/login`** → renders `login.html`.
- **GET `/register`** → renders `register.html`.
- **POST `/register`** → creates a new `AppUser` with a bcrypt hash (via `PasswordEncoder`) and redirects to login.
- Works with **`UserRepository`** to persist users and **`CustomUserDetailsService`** for authentication lookup.

**Flow**
1. User hits `/login` or `/register`.
2. On register, input is validated; password is hashed; new `AppUser` stored.
3. Spring Security handles the session after successful login.

### 3.2 CredentialController
Key routes (based on code):
- **GET `/credentials`** → list credentials for the current user → `list.html`.
- **GET `/credentials/add`** → add form → `add.html`.
- **POST `/credentials/add`** → persists a new `Credential` (service encrypts pw) → redirect.
- **GET `/credentials/view/<built-in function id>`** → decrypts and shows plain password + username → `view.html`.
- **GET `/credentials/edit/<built-in function id>`** → edit form → `edit.html`.
- **POST `/credentials/edit/<built-in function id>`** → updates fields; re-encrypts if password changed → redirect.
- **GET `/credentials/delete/<built-in function id>`** → deletes record → redirect.
- **GET/POST `/credentials/search`** → search by site/login → `search.html`.

**Flow (Add credential)**
1. Controller receives form (`site`, `loginUsername`, `password`).
2. **CredentialService** encrypts the password with the app master key and persists via **CredentialRepository**.
3. Redirects to list.

**Flow (View credential)**
1. Load by `id` (scoped to current user).
2. Decrypt password; pass `plainPassword` and `cred` to the view.
3. UI provides show/hide and copy actions (client‑side only).

### 3.3 CredentialService
- Centralizes credential CRUD and **encryption/decryption** using a master key (`APP_MASTER_KEY`, base64) or in-memory default.
- Ensures credentials are always stored encrypted at rest.

### 3.4 Models & Repositories
- **`AppUser`**: id, username, passwordHash, roles (typical fields).
- **`Credential`**: id, site, loginUsername, encryptedPassword, user reference.
- Repositories are interfaces extending `JpaRepository<…>` with finder methods (e.g., by user, search).

### 3.5 SecurityConfig & CustomUserDetailsService
- **SecurityConfig** configures HTTP security: permits login/register, protects `/credentials/**`, sets logout.
- **CustomUserDetailsService** adapts `AppUser` to Spring Security’s `UserDetails`.

---

## 4) Frontend enhancements (code pointers)

- **Theme toggle**: implemented in `/js/app.js` (`initTheme`) + styles in `/css/app.css`. Persists theme under `pm.theme`.
- **Filter & sort**: `initTableTools()` augments the list table: instant filter and header-based sort.
- **Notes modal**: `openNotes()` reads/writes to `localStorage` as `pm.notes:<id>`. Modal is a lightweight overlay—no Bootstrap JS dependency.
- **Export/Import**: `exportNotes()` and `importNotes()` (JSON).
- **Copy & Show/Hide**: `initViewPage()` wires username/password copy and toggling blur on the password.
- **Strength meter & generator**: `initAddEdit()` updates a progress bar and generates strong random passwords.

---

## 5) Deployment & environments

- **H2** in-memory DB by default; enable **MySQL** by setting:
  - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- **APP_MASTER_KEY** (base64) for AES encryption at rest.
- Docker & Kubernetes manifests are present in the repo (`Dockerfile`, `docker-compose.yml`, `k8s/*.yaml`).

---

## 6) Future extensions

- **Server-synced Notes**: add `@Lob private String notes;` to `Credential`, surface in forms & views; include in exports.
- **Tags & Favorites**: tag credentials and filter by tag; pin favorites to top.
- **2FA storage**: encrypted TOTP secrets with on-page one-time code generator.
- **Audit log**: record access timestamps per credential.
- **Pagination & bulk actions** on the list view.
