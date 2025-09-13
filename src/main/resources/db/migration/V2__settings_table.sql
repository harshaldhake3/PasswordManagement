CREATE TABLE IF NOT EXISTS settings (
  id BIGINT PRIMARY KEY,
  password_min_length INT NOT NULL DEFAULT 12,
  require_symbols BOOLEAN NOT NULL DEFAULT TRUE,
  session_timeout_minutes INT NOT NULL DEFAULT 30,
  mfa_required BOOLEAN NOT NULL DEFAULT FALSE
);
INSERT INTO settings (id, password_min_length, require_symbols, session_timeout_minutes, mfa_required)
SELECT 1, 12, TRUE, 30, FALSE
WHERE NOT EXISTS (SELECT 1 FROM settings WHERE id = 1);
