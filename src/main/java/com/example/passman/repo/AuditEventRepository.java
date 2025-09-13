package com.example.passman.repo;

import com.example.passman.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {}
