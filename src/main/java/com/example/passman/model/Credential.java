
package com.example.passman.model;

import jakarta.persistence.*;

@Entity
public class Credential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String site;
    private String loginUsername;

    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime lastUpdatedAt;
    private java.time.LocalDateTime lastViewedAt;
    private java.time.LocalDateTime passwordUpdatedAt;

    // AES-encrypted password bytes stored as Base64
    @Lob
    private String encryptedPassword;

    @ManyToOne
    private String tags;

    @Lob
    private String notes;

    private boolean favorite = false;

    private AppUser owner;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public String getLoginUsername() { return loginUsername; }
    public void setLoginUsername(String loginUsername) { this.loginUsername = loginUsername; }
    public String getEncryptedPassword() { return encryptedPassword; }
    public void setEncryptedPassword(String encryptedPassword) { this.encryptedPassword = encryptedPassword; }
    public AppUser getOwner() { return owner; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
public void setOwner(AppUser owner) { this.owner = owner; }
}
