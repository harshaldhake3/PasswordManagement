package com.example.passman.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant at = Instant.now();
    private String actor;          // username
    private String action;         // e.g., CREATED_CREDENTIAL, DELETED_CREDENTIAL
    private String targetType;     // CREDENTIAL, USER
    private String targetId;       // id as string
    private String ip;
    private String details;        // optional JSON/text

    public Long getId() { return id; }
    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
