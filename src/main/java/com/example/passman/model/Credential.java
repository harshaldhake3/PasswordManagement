
package com.example.passman.model;

import jakarta.persistence.*;

@Entity
public class Credential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String site;
    private String loginUsername;

    private String url;

    @Lob
    private String notes;

    private String tags; // comma-separated

    // AES-encrypted password bytes stored as Base64

    // AES-encrypted password bytes stored as Base64
    @Lob
    private String encryptedPassword;

    @ManyToOne
    private AppUser owner;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public String getLoginUsername() { return loginUsername; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public void setLoginUsername(String loginUsername) { this.loginUsername = loginUsername; }
    public String getEncryptedPassword() { return encryptedPassword; }
    public void setEncryptedPassword(String encryptedPassword) { this.encryptedPassword = encryptedPassword; }
    public AppUser getOwner() { return owner; }
    public void setOwner(AppUser owner) { this.owner = owner; }
}
