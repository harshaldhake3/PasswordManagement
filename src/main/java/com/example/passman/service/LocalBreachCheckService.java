package com.example.passman.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class LocalBreachCheckService implements BreachCheckService {
    @Value("${security.breach-check.enabled:true}")
    private boolean enabled;

    private static final Set<String> COMMON = Set.of(
        "password","123456","123456789","qwerty","111111","abc123","123123","iloveyou","admin","welcome"
    );

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public boolean isCompromised(String password) {
        if (!enabled || password == null) return false;
        String p = password.trim().toLowerCase();
        return COMMON.contains(p);
    }
}
