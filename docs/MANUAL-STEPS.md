# Manual Steps & Optional Enhancements

Below are the manual changes that complement the patch, with ready-to-paste code.

---
## 1) Thymeleaf Spring Security Extras (Required)
Add to `pom.xml`:
```xml
<dependency>
  <groupId>org.thymeleaf.extras</groupId>
  <artifactId>thymeleaf-extras-springsecurity6</artifactId>
</dependency>
```
Why: enables `#authorization` in Thymeleaf to show/hide menu items for roles.

---
## 2) Seed an Admin (Required once)
Use the SQL script matching your DB in `src/main/resources/db/seed/`:
- H2 (dev): `seed-admin-h2.sql`
- MySQL: `seed-admin-mysql.sql`
- PostgreSQL: `seed-admin-postgres.sql`

Replace `'youradmin'` with your real username before running.

---
## 3) Hardened Headers (Recommended for prod)
Use `config/SecurityConfigHardened.java` or merge this into your existing `SecurityConfig`:
```java
.headers(h -> h
  .contentSecurityPolicy(csp -> csp.policyDirectives(
      "default-src 'self'; " +
      "img-src 'self' data:; " +
      "script-src 'self' 'unsafe-inline' cdn.jsdelivr.net; " +
      "style-src 'self' 'unsafe-inline' cdn.jsdelivr.net; " +
      "connect-src 'self'; frame-ancestors 'self'"))
  .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
)
```
Adjust CDNs if you self-host assets.

---
## 4) TOTP MFA Quickstart (Optional)
- Add dependency:
  ```xml
  <dependency>
    <groupId>com.warrenstrange</groupId>
    <artifactId>googleauth</artifactId>
    <version>1.7.0</version>
  </dependency>
  ```
- Use the provided `MfaController` and `mfa.html` as a starter.
- Flow: After password auth, check `AppUser.mfaSecret`. If present, redirect to `/mfa` for code verification before completing login.
- Admin can **Reset MFA** per user from `/admin/users`.

---
## 5) Password Policy Persistence (Optional)
- Use the provided Flyway script `db/migration/V2__settings_table.sql` (or copy to your schema tool).
- Implement `Settings`, `SettingsRepository`, and `SettingsService` (starters provided).
- Read settings in controllers/services to enforce min length, symbols, session timeout.

---
## 6) Have I Been Pwned Check (Optional)
- Provided `HibpService` demonstrates the **k-anonymity** API usage (send **first 5** SHA-1 chars only).
- Call it when creating/updating credentials to warn users about compromised passwords.
- Add a 1–2 sec timeout and silent fallback to avoid blocking saves on network failure.

---
## 7) Audit Everything (Recommended)
- Call `auditSvc.log(...)` in your non-admin flows:
  ```java
  auditSvc.log(auth.getName(), "CREATED_CREDENTIAL", "CREDENTIAL", credential.getId(), "{site:'" + site + "'}");
  ```

---
## 8) Table Niceties (Optional)
- Column visibility toggles + CSV export can be added by including `static/js/table-extras.js` and wiring checkboxes in your tables. Starter provided.

---
## 9) Deploy Notes
- Force HTTPS and enable HSTS.
- Set a strict CSP and host static assets on your own domain where possible.
- Turn off H2 console in production.
- Set session timeout via `server.servlet.session.timeout` or your settings service.
