package com.example.passman.controller;

import com.example.passman.model.AppUser;
import com.example.passman.repo.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class MfaController {
    private final UserRepository users;
    private final GoogleAuthenticator gauth = new GoogleAuthenticator();

    public MfaController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/mfa")
    public String mfaPage(Authentication auth, Model model) {
        AppUser u = users.findByUsername(auth.getName()).orElseThrow();
        if (u.getMfaSecret() == null) {
            // Enrollment page would go here (generate secret + QR)
            model.addAttribute("enrolled", false);
        } else {
            model.addAttribute("enrolled", true);
        }
        return "mfa";
    }

    @PostMapping("/mfa/verify")
    public String verify(Authentication auth, @RequestParam int code) {
        AppUser u = users.findByUsername(auth.getName()).orElseThrow();
        if (u.getMfaSecret() == null) return "redirect:/mfa?error=no_mfa";
        boolean ok = gauth.authorize(u.getMfaSecret(), code);
        return ok ? "redirect:/credentials" : "redirect:/mfa?error=bad_code";
    }
}
