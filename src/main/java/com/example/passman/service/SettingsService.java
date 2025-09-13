package com.example.passman.service;

import com.example.passman.model.Settings;
import com.example.passman.repo.SettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {
    private final SettingsRepository repo;
    public SettingsService(SettingsRepository repo) { this.repo = repo; }
    public Settings get() { return repo.findById(1L).orElseGet(() -> repo.save(new Settings())); }
    public Settings save(Settings s) { s.setId(1L); return repo.save(s); }
}
