# Apply the PassMan v5 UI + Admin Patch

This guide explains **exactly** how to apply the patch and verify everything.

## 0) Prereqs
- Java 17+, Maven
- Your app is a Spring Boot + Thymeleaf project.
- The package base in this patch is `com.example.passman`. If your project uses a different base package, rename folders accordingly.

## 1) Backup & Unzip
1. **Back up** your project (or create a new git branch).
2. Unzip the patch into the **root** of your project, preserving paths:
   - `src/main/java/com/example/passman/...`
   - `src/main/resources/...`

## 2) Add Thymeleaf Spring Security Extras
In your `pom.xml`, add:
```xml
<dependency>
  <groupId>org.thymeleaf.extras</groupId>
  <artifactId>thymeleaf-extras-springsecurity6</artifactId>
</dependency>
```

## 3) (Optional) Enable Hardened Security Headers
The patch ships a sample at:
- `src/main/java/com/example/passman/config/SecurityConfigHardened.java`

You can **replace** your `SecurityConfig.java` with that file, or cherry-pick the `headers(...)` section.

## 4) Database Changes
The patch adds these JPA entities:
- `AppUser` (extended with `email`, `enabled`, `mfaSecret`, `roles`)
- `AuditEvent` (new)

JPA/Hibernate will create the `user_roles` and `audit_event` tables automatically if `ddl-auto` is `update/create`.

### Seeding an Admin User
Run **one** of the provided seed SQL scripts (adjust the username):
- H2 (dev): `src/main/resources/db/seed/seed-admin-h2.sql`
- MySQL: `src/main/resources/db/seed/seed-admin-mysql.sql`
- PostgreSQL: `src/main/resources/db/seed/seed-admin-postgres.sql`

Example (H2 console):  
```sql
-- Make 'youradmin' an ADMIN (replace with real username)
insert into user_roles(user_id, role)
select id, 'ADMIN' from app_user where username='youradmin';
```

## 5) Build & Run
```bash
mvn clean spring-boot:run
```

Log in as your admin user and visit **`/admin`**. You should see **Users**, **Logs**, and **Settings** tabs.

## 6) Verify Features
- My Vault table has **filter**, **sort**, **pagination**, and **copy-to-clipboard**.
- Admin → Users: enable/disable, grant/revoke admin, reset MFA, reset password (one-time toast).
- Admin → Logs: audit events listed with IPs.
- Admin → Settings: UI controls (stub) for password policy & session timeout.

---
## What’s Included (Quick Map)
- Controllers, services, repos for Admin + Audit
- New UI layout (Bootstrap 5.3, dark mode, toasts)
- Hardened security config example (CSP, HSTS)
- Optional starters for MFA & HIBP breach checks
- SQL seed scripts for H2/MySQL/PostgreSQL

If you need help adapting to a different package name or build system, copy files while preserving their relative structure and update package declarations.
