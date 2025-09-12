package com.example.passman.controller;

import com.example.passman.model.AppUser;
import com.example.passman.repo.AuditEventRepository;
import com.example.passman.repo.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/audit")
public class AuditController {
    private final AuditEventRepository auditRepo;
    private final UserRepository userRepo;

    public AuditController(AuditEventRepository auditRepo, UserRepository userRepo) {
        this.auditRepo = auditRepo; this.userRepo = userRepo;
    }

    @GetMapping
    public String list(Authentication auth, Model model) {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        model.addAttribute("events", auditRepo.findByUserOrderByAtDesc(user));
        return "audit";
    }
}
