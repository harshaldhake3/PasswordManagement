package com.example.passman.service;

import com.example.passman.model.AppUser;
import com.example.passman.model.Credential;
import com.example.passman.repo.CredentialRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/**
 * Central credential logic: crypto, helpers (strength/reuse/breach/age), and persistence utilities.
 */
@Service
public class CredentialService {

    private final CredentialRepository repo;

    @Autowired(required = false)
    private BreachCheckService breach;

    @Value("${app.master-key:}")
    private String masterKeyB64;

    @Value("${security.password.max-age-days:180}")
    private int maxAgeDays;

    private SecretKeySpec keySpec;
    private final SecureRandom rng = new SecureRandom();

    public CredentialService(CredentialRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void initKey() {
        try {
            byte[] key;
            if (masterKeyB64 != null && !masterKeyB64.isBlank()) {
                key = Base64.getDecoder().decode(masterKeyB64.trim());
            } else {
                // ephemeral key for dev unless APP master key is supplied
                key = new byte[32];
                rng.nextBytes(key);
            }
            if (key.length != 16 && key.length != 24 && key.length != 32) {
                throw new IllegalArgumentException("APP master key must be 16/24/32 bytes (Base64)");
            }
            keySpec = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize encryption key", e);
        }
    }

    /* ----------------------------- Persistence ----------------------------- */

    public List<Credential> listForUser(AppUser owner) {
        return repo.findByOwner(owner);
    }

    public Credential save(Credential c) {
        return repo.save(c);
    }

    public void delete(Credential c) {
        repo.delete(c);
    }

    /* ----------------------------- Crypto (AES-GCM) ----------------------------- */

    public String encryptPassword(String plain) throws Exception {
        byte[] iv = new byte[12];
        rng.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

        byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ct, 0, combined, iv.length, ct.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    public String decryptPassword(Credential c) throws Exception {
        String enc = c.getEncryptedPassword();
        if (enc == null || enc.isBlank()) return "";
        byte[] combined = Base64.getDecoder().decode(enc);
        if (combined.length < 13) throw new IllegalArgumentException("ciphertext too short");

        byte[] iv = Arrays.copyOfRange(combined, 0, 12);
        byte[] ct = Arrays.copyOfRange(combined, 12, combined.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
        byte[] pt = cipher.doFinal(ct);
        return new String(pt, StandardCharsets.UTF_8);
    }

    /* ----------------------------- UX helpers ----------------------------- */

    /** Returns 0..4 score */
    public int passwordStrength(String s) {
        int score = 0;
        if (s == null) return 0;
        if (s.length() >= 12) score++;
        if (s.matches(".*[A-Z].*") && s.matches(".*[a-z].*")) score++;
        if (s.matches(".*[0-9].*")) score++;
        if (s.matches(".*[^A-Za-z0-9].*")) score++;
        return score;
    }

    public Map<Long, Integer> strengthById(List<Credential> creds) {
        Map<Long, Integer> out = new HashMap<>();
        for (Credential c : creds) {
            try {
                out.put(c.getId(), passwordStrength(decryptPassword(c)));
            } catch (Exception e) {
                out.put(c.getId(), 0);
            }
        }
        return out;
    }

    /** Set of plaintexts used ≥2 times */
    public Set<String> reusedPasswords(List<Credential> creds) {
        Map<String, Integer> counts = new HashMap<>();
        for (Credential c : creds) {
            try {
                String p = decryptPassword(c);
                counts.put(p, counts.getOrDefault(p, 0) + 1);
            } catch (Exception ignored) {}
        }
        Set<String> reused = new HashSet<>();
        for (var e : counts.entrySet()) if (e.getValue() > 1) reused.add(e.getKey());
        return reused;
    }

    /** IDs flagged by breach checker (very common passwords, or via HIBP later) */
    public Set<Long> compromisedIds(List<Credential> creds) {
        Set<Long> out = new HashSet<>();
        if (breach == null || !breach.isEnabled()) return out;
        for (Credential c : creds) {
            try {
                if (breach.isCompromised(decryptPassword(c))) out.add(c.getId());
            } catch (Exception ignored) {}
        }
        return out;
    }

    /** IDs whose password age exceeds policy */
    public Set<Long> staleIds(List<Credential> creds) {
        Set<Long> out = new HashSet<>();
        if (maxAgeDays <= 0) return out;
        var cutoff = java.time.LocalDateTime.now().minusDays(maxAgeDays);
        for (Credential c : creds) {
            if (c.getPasswordUpdatedAt() != null && c.getPasswordUpdatedAt().isBefore(cutoff)) {
                out.add(c.getId());
            }
        }
        return out;
    }

    /* ----------------------------- Timestamps ----------------------------- */

    public void touchCreate(Credential c) {
        var now = java.time.LocalDateTime.now();
        c.setCreatedAt(now);
        c.setLastUpdatedAt(now);
        c.setPasswordUpdatedAt(now);
    }

    public void touchUpdate(Credential c) {
        c.setLastUpdatedAt(java.time.LocalDateTime.now());
    }

    public void touchPasswordRotate(Credential c) {
        c.setPasswordUpdatedAt(java.time.LocalDateTime.now());
    }
}
