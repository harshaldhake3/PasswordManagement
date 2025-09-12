
package com.example.passman.service;

import com.example.passman.model.AppUser;
import com.example.passman.model.Credential;
import com.example.passman.repo.CredentialRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@Service
public class CredentialService {

    @Autowired private BreachCheckService breach;
    @Value("${security.password.max-age-days:180}") private int maxAgeDays;


    private java.time.Clock clock = java.time.Clock.systemDefaultZone();

    public void touchCreate(Credential c) { c.setCreatedAt(java.time.LocalDateTime.now(clock)); c.setLastUpdatedAt(java.time.LocalDateTime.now(clock)); c.setPasswordUpdatedAt(java.time.LocalDateTime.now(clock)); }
    public void touchUpdate(Credential c) { c.setLastUpdatedAt(java.time.LocalDateTime.now(clock)); }
    public void touchPasswordRotate(Credential c) { c.setPasswordUpdatedAt(java.time.LocalDateTime.now(clock)); }

    public int passwordStrength(String s) {
        int score = 0;
        if (s == null) return 0;
        if (s.length() >= 12) score++;
        if (s.matches(".*[A-Z].*") && s.matches(".*[a-z].*")) score++;
        if (s.matches(".*[0-9].*")) score++;
        if (s.matches(".*[^A-Za-z0-9].*")) score++;
        return score; // 0..4
    }

    public Map<Long, Integer> strengthById(List<Credential> creds) {
        Map<Long, Integer> out = new HashMap<>();
        for (Credential c : creds) {
            try { out.put(c.getId(), passwordStrength(decryptPassword(c))); } catch (Exception ex) { out.put(c.getId(), 0); }
        }
        return out;
    }

    public Set<String> reusedPasswords(List<Credential> creds) {
        Map<String, Integer> counts = new HashMap<>();
        for (Credential c : creds) {
            try {
                String p = decryptPassword(c);
                counts.put(p, counts.getOrDefault(p, 0)+1);
            } catch (Exception ignored) {}
        }
        Set<String> reused = new HashSet<>();
        for (var e : counts.entrySet()) if (e.getValue() > 1) reused.add(e.getKey());
        return reused;
    }


    private final CredentialRepository repo;

    // AES key provided by env var MASTER_KEY (must be 16/24/32 bytes base64). If missing, service will generate a temporary key (not for production).
    @Value("${app.master-key:}")
    private String masterKeyEnv;

    private SecretKeySpec keySpec;

    public CredentialService(CredentialRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void init() {
        try {
            if (masterKeyEnv == null || masterKeyEnv.isEmpty()) {
                // generate random 16-byte key (warning in logs)
                byte[] k = new byte[16];
                new SecureRandom().nextBytes(k);
                keySpec = new SecretKeySpec(k, "AES");
                System.out.println("WARNING: No MASTER_KEY provided. Using a generated ephemeral key (NOT for production).");
            } else {
                byte[] decoded = Base64.getDecoder().decode(masterKeyEnv);
                keySpec = new SecretKeySpec(decoded, "AES");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Credential saveCredential(AppUser owner, String site, String loginUsername, String plainPassword) throws Exception {
        Credential c = new Credential();
        c.setOwner(owner);
        c.setSite(site);
        c.setLoginUsername(loginUsername);
        c.setEncryptedPassword(encrypt(plainPassword));
        return repo.save(c);
    }

    public List<Credential> listForUser(AppUser owner) {
        return repo.findByOwner(owner);
    }

    public String decryptPassword(Credential c) throws Exception {
        return decrypt(c.getEncryptedPassword());
    }

    // AES-GCM encrypt/decrypt
    private String encrypt(String plain) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
        byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + ct.length];
        System.arraycopy(iv,0,combined,0,iv.length);
        System.arraycopy(ct,0,combined,iv.length,ct.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    private String decrypt(String combinedB64) throws Exception {
        byte[] combined = Base64.getDecoder().decode(combinedB64);
        byte[] iv = new byte[12];
        System.arraycopy(combined,0,iv,0,12);
        byte[] ct = new byte[combined.length - 12];
        System.arraycopy(combined,12,ct,0,ct.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
        byte[] plain = cipher.doFinal(ct);
        return new String(plain, StandardCharsets.UTF_8);
    }


    public void delete(Credential c) {
        repo.delete(c);
    }
}

    public Set<Long> compromisedIds(List<Credential> creds) {
        Set<Long> out = new HashSet<>();
        for (Credential c : creds) {
            try {
                String p = decryptPassword(c);
                if (breach != null && breach.isEnabled() && breach.isCompromised(p)) out.add(c.getId());
            } catch (Exception ignored) {}
        }
        return out;
    }

    public Set<Long> staleIds(List<Credential> creds) {
        Set<Long> out = new HashSet<>();
        if (maxAgeDays <= 0) return out;
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(maxAgeDays);
        for (Credential c : creds) {
            if (c.getPasswordUpdatedAt() != null && c.getPasswordUpdatedAt().isBefore(cutoff)) {
                out.add(c.getId());
            }
        }
        return out;
    }
