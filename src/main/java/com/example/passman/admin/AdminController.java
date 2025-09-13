package com.example.passman.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

import static com.example.passman.admin.AdminDTOs.*;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final JdbcTemplate jdbc;

    public AdminController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        Integer users = jdbc.queryForObject("SELECT COUNT(*) FROM app_user", Integer.class);
        Integer creds = jdbc.queryForObject("SELECT COUNT(*) FROM credential", Integer.class);
        model.addAttribute("usersCount", users);
        model.addAttribute("credsCount", creds);
        return "admin/admin_dashboard";
    }

    @GetMapping("/admin/users")
    public String users(Model model) {
        List<UserRow> rows = jdbc.query("SELECT id, username, COALESCE(role,'USER') AS role FROM app_user ORDER BY id DESC",
                (rs, i) -> new UserRow(rs.getLong("id"), rs.getString("username"), rs.getString("role")));
        model.addAttribute("rows", rows);
        return "admin/admin_users";
    }

    @GetMapping("/admin/credentials")
    public String credentials(Model model) {
        List<CredRow> rows = jdbc.query("SELECT id, site, login, COALESCE(url,'') AS url, COALESCE(tags,'') AS tags FROM credential ORDER BY id DESC",
                (rs, i) -> new CredRow(rs.getLong("id"), rs.getString("site"), rs.getString("login"), rs.getString("url"), rs.getString("tags")));
        model.addAttribute("rows", rows);
        return "admin/admin_credentials";
    }

    @GetMapping("/admin/sql")
    public String sql(Model model) {
        model.addAttribute("rows", List.of());
        model.addAttribute("cols", List.of());
        model.addAttribute("error", null);
        return "admin/admin_sql";
    }

    @PostMapping("/admin/sql")
    public String runSql(@RequestParam("q") String q, Model model) {
        if (!SqlSafety.isSelectOnly(q)) {
            model.addAttribute("error", "Only SELECT statements are allowed.");
            model.addAttribute("rows", List.of());
            model.addAttribute("cols", List.of());
            return "admin/admin_sql";
        }
        String limited = SqlSafety.enforceLimit(q, 200);
        List<Map<String,Object>> results = jdbc.queryForList(limited);
        List<String> cols = results.isEmpty() ? List.of() : results.get(0).keySet().stream().toList();
        model.addAttribute("rows", results);
        model.addAttribute("cols", cols);
        model.addAttribute("error", null);
        model.addAttribute("q", q);
        return "admin/admin_sql";
    }
}
