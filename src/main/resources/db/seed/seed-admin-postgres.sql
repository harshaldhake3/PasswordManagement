-- PostgreSQL: make an existing user admin (replace username)
INSERT INTO user_roles(user_id, role)
SELECT id, 'ADMIN' FROM app_user WHERE username='youradmin';
