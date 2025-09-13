
-- Default admin user (change password immediately!)
INSERT INTO app_user (id, username, password_hash, role) 
SELECT 1, 'admin', '$2a$10$t1N6m7sQJX2V0QdYH1oXkOPn8G5kS0mC4yWzI0Qx0sYwknnZ9m0F.', 'ROLE_ADMIN' 
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username='admin');
