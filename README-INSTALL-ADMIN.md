# Admin Panel Add‑on (drop‑in)

This folder contains a minimal, read‑only admin module for your Spring Boot + Thymeleaf password manager.

## What it includes
- Routes (ROLE_ADMIN required):
  - `/admin/dashboard` — totals
  - `/admin/users` — list users (id, username, role)
  - `/admin/credentials` — list credentials metadata
  - `/admin/sql` — **read‑only** SQL runner (SELECT only, LIMIT 200)
- Static assets (`/css/admin.css`, `/js/admin.js`)
- Thin `JdbcTemplate` based DAO to avoid coupling with your existing repositories

## How to install
1) Copy all folders inside `src/` into your project `src/` (merge paths).
2) Ensure Thymeleaf can see templates under `templates/admin/`.
3) Make sure your security config protects `/admin/**` with `hasRole('ADMIN')` and allows login/register and static assets, e.g.:

```java
http
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/login","/register","/css/**","/js/**","/images/**","/h2-console/**").permitAll()
      .requestMatchers("/admin/**").hasRole("ADMIN")
      .anyRequest().authenticated()
  )
  .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/credentials", true).permitAll())
  .logout(logout -> logout.permitAll());
http.headers().frameOptions().sameOrigin();
```

4) Your `AppUser` should have a `role` column with values like `ROLE_USER` or `ROLE_ADMIN`, and your `UserDetailsService` must expose that role.
   Example column: `ALTER TABLE app_user ADD COLUMN role VARCHAR(32) DEFAULT 'ROLE_USER';`

5) Seed an admin user if you do not have one yet (H2 quick example):

```sql
INSERT INTO app_user (username, password_hash, role) VALUES ('admin', '$2a$10$7SIp7u7fmi3z9a6l7Y0mUu6kC2n8L1cZxk9Qb7jMy9zqgLq5vJrLe', 'ROLE_ADMIN');
-- password = admin
```

## Notes
- The SQL runner blocks non‑SELECT statements for safety.
- If your table names differ, adjust the queries in `AdminController.java` accordingly.
