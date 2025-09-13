
package com.example.passman.controller;

import com.example.passman.model.AppUser;
import com.example.passman.model.Credential;
import com.example.passman.repo.UserRepository;
import com.example.passman.repo.CredentialRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository users;
    private final CredentialRepository creds;
    private final JdbcTemplate jdbc;

    public AdminController(UserRepository users, CredentialRepository creds, JdbcTemplate jdbc) {
        this.users = users; this.creds = creds; this.jdbc = jdbc;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("userCount", users.count());
        model.addAttribute("credCount", creds.count());
        return "admin_dashboard";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", users.findAll());
        return "admin_users";
    }

    @GetMapping("/credentials")
    public String credentials(Model model) {
        model.addAttribute("creds", creds.findAll());
        return "admin_credentials";
    }

    @GetMapping("/sql")
    public String sqlForm() { return "admin_sql"; }

    @PostMapping("/sql")
    public String runSql(@RequestParam String query, Model model) {
        String q = query.trim();
        if (!q.toLowerCase().startsWith("select")) {
            model.addAttribute("error","Only SELECT queries are allowed.");
            return "admin_sql";
        }
        if (q.toLowerCase().contains("delete") || q.toLowerCase().contains("update") || q.toLowerCase().contains("insert") || q.toLowerCase().contains("drop")) {
            model.addAttribute("error","Write operations are not allowed.");
            return "admin_sql";
        }
        List<Map<String,Object>> rows = jdbc.queryForList(q + " LIMIT 200");
        model.addAttribute("rows", rows);
        model.addAttribute("query", query);
        return "admin_sql";
    }
}
