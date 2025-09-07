
# Passman - Minimal Password Management App

Features:
- Spring Boot + Thymeleaf UI
- User registration and login (BCrypt hashed passwords)
- Store credentials (site, username, encrypted password using AES-GCM)
- Dockerfile, docker-compose, and Kubernetes manifest included
- H2 in-memory DB used by default; configure MySQL via environment variables for persistence.

How to run:
- Local (H2 memory): `mvn spring-boot:run`
- Docker build: `docker build -t passman . && docker run -p 8080:8080 passman`
- Docker Compose: `docker-compose up --build`
- Kubernetes: modify image in k8s/passman-deployment.yaml and apply with `kubectl apply -f k8s/passman-deployment.yaml`

Security notes:
- Set a stable APP_MASTER_KEY environment variable (base64) to ensure stored credentials remain decryptable across restarts.
- Do NOT use the generated ephemeral key in production (the app logs a warning if APP_MASTER_KEY is not set).


## Production Kubernetes deployment notes

1. Replace `passman-secrets.app-master-key` with a secure base64-encoded AES key (16/24/32 bytes before base64 encoding).
2. Build & push the app image:
   ```bash
   docker build -t your-docker-registry/passman:latest .
   docker push your-docker-registry/passman:latest
   ```
3. Apply k8s manifests:
   ```bash
   kubectl apply -f k8s/production.yaml
   ```
4. Ensure an ingress controller (e.g., nginx) is installed and configure DNS for `passman.example.com` or replace with your domain.
5. Use a Kubernetes secret manager (Vault, SealedSecrets, or cloud KMS) for production secret management.
# PasswordManagement
