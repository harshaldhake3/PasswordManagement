
package com.example.passman.controller;

import com.example.passman.model.AppUser;
import com.example.passman.model.Credential;
import com.example.passman.repo.UserRepository;
import com.example.passman.service.CredentialService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;

@Controller
@RequestMapping("/credentials")
public class CredentialController {

    private final UserRepository userRepo;
    private final CredentialService credSvc;

    public CredentialController(UserRepository userRepo, CredentialService credSvc) {
        this.userRepo = userRepo;
        this.credSvc = credSvc;
    }

    @GetMapping
    public String list(Authentication auth, Model model) throws Exception {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        var list = credSvc.listForUser(user);
        model.addAttribute("creds", list);
        return "list";
    }

    @GetMapping("/add")
    public String addForm() { return "add"; }

    @PostMapping("/add")
    public String add(Authentication auth, @RequestParam String site, @RequestParam String loginUsername, @RequestParam String password, Model model) throws Exception {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        credSvc.saveCredential(user, site, loginUsername, password);
        return "redirect:/credentials";
    }

    @GetMapping("/view/{id}")
    public String view(Authentication auth, @PathVariable Long id, Model model) throws Exception {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        var list = credSvc.listForUser(user);
        Credential found = list.stream().filter(c->c.getId().equals(id)).findFirst().orElseThrow();
        String plain = credSvc.decryptPassword(found);
        model.addAttribute("cred", found);
        model.addAttribute("plainPassword", plain);
        return "view";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Authentication auth, Model model) {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        Credential cred = credSvc.listForUser(user).stream()
            .filter(c -> c.getId().equals(id)).findFirst().orElseThrow();
        model.addAttribute("cred", cred);
        return "edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Authentication auth,
                       @RequestParam String site, @RequestParam String loginUsername,
                       @RequestParam String password) throws Exception {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        Credential cred = credSvc.listForUser(user).stream()
            .filter(c -> c.getId().equals(id)).findFirst().orElseThrow();
        cred.setSite(site);
        cred.setLoginUsername(loginUsername);
        if (!password.isEmpty()) {
            cred.setEncryptedPassword(credSvc.saveCredential(user, site, loginUsername, password).getEncryptedPassword());
        }
        return "redirect:/credentials";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Authentication auth) {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        credSvc.listForUser(user).stream()
            .filter(c -> c.getId().equals(id)).findFirst()
            .ifPresent(c -> credSvc.delete(c));
        return "redirect:/credentials";
    }

    @GetMapping("/search")
    public String searchForm() { return "search"; }

    @PostMapping("/search")
    public String search(Authentication auth, @RequestParam String query, Model model) {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        var results = credSvc.listForUser(user).stream()
            .filter(c -> c.getSite().toLowerCase().contains(query.toLowerCase()) ||
                         c.getLoginUsername().toLowerCase().contains(query.toLowerCase()))
            .toList();
        model.addAttribute("creds", results);
        return "list";
    }

    @GetMapping("/export")
    public void export(Authentication auth, HttpServletResponse response) throws Exception {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        var creds = credSvc.listForUser(user);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=credentials.csv");

        PrintWriter writer = response.getWriter();
        writer.println("Site,Username,Password");
        for (var c : creds) {
            writer.printf("%s,%s,%s%n", c.getSite(), c.getLoginUsername(), credSvc.decryptPassword(c));
        }
        writer.flush();
    }
}
