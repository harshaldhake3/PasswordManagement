package com.example.passman.repo;

import com.example.passman.model.AuditEvent;
import com.example.passman.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByUserOrderByAtDesc(AppUser user);
}
