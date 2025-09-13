package com.example.passman.controller;

import com.example.passman.model.AppUser;
import com.example.passman.model.Role;
import com.example.passman.repo.UserRepository;
import com.example.passman.repo.AuditEventRepository;
import com.example.passman.service.AuditService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Base64;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserRepository users;
    private final AuditEventRepository audits;
    private final PasswordEncoder encoder;
    private final AuditService auditSvc;

    public AdminController(UserRepository users, AuditEventRepository audits, PasswordEncoder encoder, AuditService auditSvc) {
        this.users = users;
        this.audits = audits;
        this.encoder = encoder;
        this.auditSvc = auditSvc;
    }

    @GetMapping
    public String index() { return "admin/index"; }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", users.findAll());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggle(@PathVariable Long id, @RequestParam String actor) {
        var u = users.findById(id).orElseThrow();
        u.setEnabled(!u.isEnabled());
        users.save(u);
        auditSvc.log(actor, "TOGGLE_USER", "USER", id, "enabled=" + u.isEnabled());
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/makeAdmin")
    public String makeAdmin(@PathVariable Long id, @RequestParam String actor) {
        var u = users.findById(id).orElseThrow();
        u.getRoles().add(Role.ADMIN);
        users.save(u);
        auditSvc.log(actor, "GRANT_ADMIN", "USER", id, null);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/removeAdmin")
    public String removeAdmin(@PathVariable Long id, @RequestParam String actor) {
        var u = users.findById(id).orElseThrow();
        u.getRoles().remove(Role.ADMIN);
        users.save(u);
        auditSvc.log(actor, "REVOKE_ADMIN", "USER", id, null);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/resetPassword")
    @ResponseBody
    public String resetPassword(@PathVariable Long id, @RequestParam(defaultValue = "12") int len, @RequestParam String actor) {
        var u = users.findById(id).orElseThrow();
        var pwd = randomPassword(len);
        u.setPasswordHash(encoder.encode(pwd));
        users.save(u);
        auditSvc.log(actor, "RESET_PASSWORD", "USER", id, null);
        return pwd; // show once (admin should share securely)
    }

    @PostMapping("/users/{id}/resetMfa")
    public String resetMfa(@PathVariable Long id, @RequestParam String actor) {
        var u = users.findById(id).orElseThrow();
        u.setMfaSecret(null);
        users.save(u);
        auditSvc.log(actor, "RESET_MFA", "USER", id, null);
        return "redirect:/admin/users";
    }

    @GetMapping("/logs")
    public String logs(Model model) {
        model.addAttribute("logs", audits.findAll());
        return "admin/logs";
    }

    // Settings page could later back with persistence; for now just a stub UI
    @GetMapping("/settings")
    public String settings() { return "admin/settings"; }

    private static String randomPassword(int len) {
        byte[] b = new byte[len];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b)[:len];
    }
}
