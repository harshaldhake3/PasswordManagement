package com.example.passman.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Settings {
    @Id
    private Long id = 1L;
    private int passwordMinLength = 12;
    private boolean requireSymbols = true;
    private int sessionTimeoutMinutes = 30;
    private boolean mfaRequired = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getPasswordMinLength() { return passwordMinLength; }
    public void setPasswordMinLength(int v) { this.passwordMinLength = v; }
    public boolean isRequireSymbols() { return requireSymbols; }
    public void setRequireSymbols(boolean v) { this.requireSymbols = v; }
    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public void setSessionTimeoutMinutes(int v) { this.sessionTimeoutMinutes = v; }
    public boolean isMfaRequired() { return mfaRequired; }
    public void setMfaRequired(boolean v) { this.mfaRequired = v; }
}
