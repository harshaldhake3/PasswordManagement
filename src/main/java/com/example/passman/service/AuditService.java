package com.example.passman.service;

import com.example.passman.model.AuditEvent;
import com.example.passman.model.AppUser;
import com.example.passman.repo.AuditEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {
    private final AuditEventRepository repo;
    public AuditService(AuditEventRepository repo) { this.repo = repo; }

    public void log(AppUser user, String action, Long credentialId, String details) {
        try {
            AuditEvent e = new AuditEvent();
            e.setUser(user);
            e.setAction(action);
            e.setCredentialId(credentialId);
            e.setDetails(details);
            e.setAt(LocalDateTime.now());
            repo.save(e);
        } catch (Exception ignored) {}
    }
}
