# PassMan v5 Patch: Production-grade UI + Admin Features

This patch adds:
- Admin area (`/admin`) with **User Management**, **Audit Logs**, and **Settings** (stub).
- Role-based access control (USER/ADMIN).
- Audit logging for critical admin actions.
- Upgraded responsive layout with dark mode, searchable/sortable/paginated tables, clipboard copy, and toasts.

## Apply
Copy the files into your Spring Boot project preserving paths, then rebuild.

Key modified/added paths:
- `src/main/java/com/example/passman/model/{Role.java, AppUser.java, AuditEvent.java}`
- `src/main/java/com/example/passman/repo/AuditEventRepository.java`
- `src/main/java/com/example/passman/service/AuditService.java`
- `src/main/java/com/example/passman/controller/AdminController.java`
- `src/main/java/com/example/passman/config/SecurityConfig.java` (replaces existing)
- `src/main/resources/templates/fragments/layout.html` (new)
- `src/main/resources/templates/{list.html}` (replaced)
- `src/main/resources/templates/admin/{index,users,logs,settings}.html` (new)
- `src/main/resources/static/{css/app.css, js/app.js}` (replaced)

Ensure your `UserRepository` exposes `findAll()` and `findById()` (JPA default). If you previously had an `AppUser` without roles, JPA will create the `user_roles` join table on startup.

## Next Steps
- Wire audit logging into non-admin flows (create/edit/delete credential).
- Persist settings in a `settings` table and apply to validation/security on save & login.
- Add TOTP MFA: store secret in `mfaSecret`, enforce second factor at login, show QR code.
- Add breach check integration (k-Anonymity to Have I Been Pwned) and domain-level policy.
