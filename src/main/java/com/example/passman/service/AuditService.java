package com.example.passman.service;

import com.example.passman.model.AuditEvent;
import com.example.passman.repo.AuditEventRepository;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditService {
    private final AuditEventRepository repo;
    private final HttpServletRequest request;

    public AuditService(AuditEventRepository repo, HttpServletRequest request) {
        this.repo = repo;
        this.request = request;
    }

    public void log(String actor, String action, String targetType, Object targetId, String details) {
        AuditEvent e = new AuditEvent();
        e.setActor(actor);
        e.setAction(action);
        e.setTargetType(targetType);
        e.setTargetId(targetId == null ? null : targetId.toString());
        e.setIp(request.getRemoteAddr());
        e.setDetails(details);
        repo.save(e);
    }
}
