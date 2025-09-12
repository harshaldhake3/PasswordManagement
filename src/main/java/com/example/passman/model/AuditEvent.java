package com.example.passman.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false)
    private AppUser user;

    private String action;          // e.g., VIEW, REVEAL, ADD, EDIT, DELETE, EXPORT, IMPORT
    private Long credentialId;      // nullable
    @Lob
    private String details;         // optional context
    private LocalDateTime at;

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Long getCredentialId() { return credentialId; }
    public void setCredentialId(Long credentialId) { this.credentialId = credentialId; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getAt() { return at; }
    public void setAt(LocalDateTime at) { this.at = at; }
}
