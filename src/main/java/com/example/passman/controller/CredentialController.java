
package com.example.passman.controller;

import com.example.passman.model.AppUser;
import com.example.passman.model.Credential;
import com.example.passman.repo.CredentialRepository;
import com.example.passman.repo.UserRepository;
import com.example.passman.service.AuditService;
import com.example.passman.service.CredentialService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/credentials")
public class CredentialController {
    private final CredentialService credSvc;
    private final CredentialRepository credRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    @Value("${security.encoder:bcrypt}")
    private String encoderType;

    public CredentialController(CredentialService credSvc, CredentialRepository credRepo, UserRepository userRepo, PasswordEncoder encoder, AuditService audit) {
        this.credSvc = credSvc;
        this.credRepo = credRepo;
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.audit = audit;
    }

    @GetMapping
    public String list(Authentication auth, Model model) {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        List<Credential> creds = credRepo.findByOwner(user);
        // Strength & reuse
        Map<Long,Integer> strengths = credSvc.strengthById(creds);
        Set<String> reused = credSvc.reusedPasswords(creds);
        Set<Long> compromisedIds = credSvc.compromisedIds(creds);
        Set<Long> staleIds = credSvc.staleIds(creds);
        Set<Long> reusedIds = creds.stream().filter(c -> {
            try { return reused.contains(credSvc.decryptPassword(c)); } catch(Exception e) { return false; }
        }).map(Credential::getId).collect(Collectors.toSet());
        model.addAttribute("creds", creds);
        model.addAttribute("strengths", strengths);
        model.addAttribute("reusedIds", reusedIds);
        model.addAttribute("compromisedIds", compromisedIds);
        model.addAttribute("staleIds", staleIds);
        return "list";
    }

    @GetMapping("/add")
    public String addForm() { return "add"; }

    @PostMapping("/add")
    public String add(Authentication auth,
                      @RequestParam String site,
                      @RequestParam String loginUsername,
                      @RequestParam String password,
                      @RequestParam(required=false) String tags,
                      @RequestParam(required=false, defaultValue="false") boolean favorite,
                      @RequestParam(required=false) String notes,
                      Model model) throws Exception {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        Credential c = new Credential();
        c.setSite(site);
        c.setLoginUsername(loginUsername);
        c.setEncryptedPassword(credSvc.encryptPassword(password));
        c.setOwner(user);
        c.setTags(tags);
        c.setFavorite(favorite);
        c.setNotes(notes);
        credSvc.touchCreate(c);
        credRepo.save(c);
        audit.log(user, "ADD", c.getId(), c.getSite());
        audit.log(user, "IMPORT_CSV", null, "");
        return "redirect:/credentials";
    }

    @GetMapping("/view/{id}")
    public String view(Authentication auth, @PathVariable Long id, Model model) {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        Credential cred = credRepo.findById(id).orElseThrow();
        if (!cred.getOwner().getId().equals(user.getId())) throw new RuntimeException("Forbidden");
        model.addAttribute("cred", cred);
        // Do not include plain password by default
        return "view";
    }

    @PostMapping("/view/{id}/reveal")
    public String reveal(Authentication auth, @PathVariable Long id, @RequestParam String confirmPassword, Model model) throws Exception {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        Credential cred = credRepo.findById(id).orElseThrow();
        if (!cred.getOwner().getId().equals(user.getId())) throw new RuntimeException("Forbidden");

        if (!encoder.matches(confirmPassword, user.getPasswordHash())) {
            model.addAttribute("cred", cred);
            model.addAttribute("error", "Incorrect password");
            return "view";
        }
        String plain = credSvc.decryptPassword(cred);
        cred.setLastViewedAt(java.time.LocalDateTime.now());
        credRepo.save(cred);
        model.addAttribute("cred", cred);
        model.addAttribute("plainPassword", plain);
        return "view";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Authentication auth, Model model) {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        Credential cred = credRepo.findById(id).orElseThrow();
        if (!cred.getOwner().getId().equals(user.getId())) throw new RuntimeException("Forbidden");
        model.addAttribute("cred", cred);
        return "edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                       Authentication auth,
                       @RequestParam String site,
                       @RequestParam String loginUsername,
                       @RequestParam(required=false) String password,
                       @RequestParam(required=false) String tags,
                       @RequestParam(required=false, defaultValue="false") boolean favorite,
                       @RequestParam(required=false) String notes) throws Exception {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        Credential cred = credRepo.findById(id).orElseThrow();
        if (!cred.getOwner().getId().equals(user.getId())) throw new RuntimeException("Forbidden");

        cred.setSite(site);
        cred.setLoginUsername(loginUsername);
        if (password != null && !password.isBlank()) {
            cred.setEncryptedPassword(credSvc.encryptPassword(password));
        credSvc.touchPasswordRotate(cred);
        }
        cred.setTags(tags);
        cred.setFavorite(favorite);
        cred.setNotes(notes);
        credSvc.touchUpdate(cred);
        credRepo.save(cred);
        audit.log(user, "EDIT", cred.getId(), cred.getSite());
        audit.log(user, "IMPORT_CSV", null, "");
        return "redirect:/credentials";
    }

    @PostMapping("/toggle-favorite/{id}")
    public String toggleFavorite(@PathVariable Long id, Authentication auth) {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        Credential cred = credRepo.findById(id).orElseThrow();
        if (!cred.getOwner().getId().equals(user.getId())) throw new RuntimeException("Forbidden");
        cred.setFavorite(!cred.isFavorite());
        credRepo.save(cred);
        audit.log(user, "EDIT", cred.getId(), cred.getSite());
        audit.log(user, "IMPORT_CSV", null, "");
        return "redirect:/credentials";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Authentication auth) {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        Credential cred = credRepo.findById(id).orElseThrow();
        if (!cred.getOwner().getId().equals(user.getId())) throw new RuntimeException("Forbidden");
        credRepo.delete(cred);
        audit.log(user, "DELETE", cred.getId(), cred.getSite());
        audit.log(user, "IMPORT_CSV", null, "");
        return "redirect:/credentials";
    }

    @GetMapping("/search")
    public String searchForm() { return "search"; }

    @PostMapping("/search")
    public String search(Authentication auth, @RequestParam String query, Model model) throws Exception {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        List<Credential> creds = credRepo.findByOwner(user);
        String q = query.toLowerCase(Locale.ROOT);
        creds = creds.stream().filter(c ->
            (c.getSite()!=null && c.getSite().toLowerCase().contains(q)) ||
            (c.getLoginUsername()!=null && c.getLoginUsername().toLowerCase().contains(q)) ||
            (c.getTags()!=null && c.getTags().toLowerCase().contains(q)) ||
            (c.getNotes()!=null && c.getNotes().toLowerCase().contains(q))
        ).collect(Collectors.toList());
        model.addAttribute("creds", creds);
        return "list";
    }

    @GetMapping("/export")
    public void exportCsv(Authentication auth, HttpServletResponse response) throws Exception {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        var creds = credRepo.findByOwner(user);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=credentials.csv");

        PrintWriter writer = response.getWriter();
        writer.println("Site,Username,Password,Tags,Favorite,Notes");
        for (var c : creds) {
            String plain = "";
            try { plain = credSvc.decryptPassword(c); } catch(Exception ignored) { }
            writer.printf("%s,%s,%s,%s,%s,%s%n",
                safe(c.getSite()), safe(c.getLoginUsername()), safe(plain), safe(c.getTags()),
                c.isFavorite() ? "true":"false", safe(c.getNotes()));
        }
        writer.flush();
    }

    private String safe(String s) { return s==null?"":s.replace(","," ").replace("\n"," ").replace("\r"," "); }

    @GetMapping("/import")
    public String importForm() { return "import"; }

    @PostMapping("/import")
    public String importCsv(Authentication auth, @RequestParam("file") MultipartFile file) throws Exception {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        String text = new String(file.getBytes());
        // Expect header: Site,Username,Password,Tags,Favorite,Notes
        String[] lines = text.split("\r?\n");
        for (int i=1; i<lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] cols = line.split(",", -1);
            if (cols.length < 3) continue;
            String site = cols[0].trim();
            String login = cols[1].trim();
            String pass = cols[2].trim();
            String tags = cols.length>3 ? cols[3].trim() : null;
            boolean favorite = (cols.length>4 && "true".equalsIgnoreCase(cols[4].trim()));
            String notes = cols.length>5 ? cols[5].trim() : null;
            Credential c = new Credential();
            c.setSite(site);
            c.setLoginUsername(login);
            try { c.setEncryptedPassword(credSvc.encryptPassword(pass)); } catch(Exception ex) { continue; }
            c.setTags(tags); c.setFavorite(favorite); c.setNotes(notes);
            c.setOwner(user);
            credSvc.touchCreate(c);
            credRepo.save(c);
        }
        audit.log(user, "IMPORT_CSV", null, "");
        return "redirect:/credentials";
    }

    @GetMapping("/export-json")
    public void exportJson(Authentication auth, HttpServletResponse response) throws Exception {
        AppUser user = userRepo.findByUsername(auth.getName()).orElseThrow();
        var creds = credRepo.findByOwner(user);
        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename=credentials.json");
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i=0;i<creds.size();i++) {
            var c = creds.get(i);
            String plain = "";
            try { plain = credSvc.decryptPassword(c); } catch(Exception ignored) { }
            sb.append("  {")
              .append("\"id\":"+c.getId()+",")
              .append("\"site\":\""+(c.getSite()==null?"":c.getSite().replace("\"","\\\""))+"\",")
              .append("\"username\":\""+(c.getLoginUsername()==null?"":c.getLoginUsername().replace("\"","\\\""))+"\",")
              .append("\"password\":\""+plain.replace("\"","\\\"")+"\",")
              .append("\"tags\":\""+((c.getTags()==null?"":c.getTags()).replace("\"","\\\""))+"\",")
              .append("\"favorite\":"+(c.isFavorite()?"true":"false")+",")
              .append("\"notes\":\""+((c.getNotes()==null?"":c.getNotes()).replace("\"","\\\""))+"\",")
              .append("\"createdAt\":\""+(c.getCreatedAt()==null?"":c.getCreatedAt())+"\",")
              .append("\"lastUpdatedAt\":\""+(c.getLastUpdatedAt()==null?"":c.getLastUpdatedAt())+"\",")
              .append("\"lastViewedAt\":\""+(c.getLastViewedAt()==null?"":c.getLastViewedAt())+"\",")
              .append("\"passwordUpdatedAt\":\""+(c.getPasswordUpdatedAt()==null?"":c.getPasswordUpdatedAt())+"\"");
            sb.append("  }");
            if (i < creds.size()-1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        response.getWriter().write(sb.toString());
        audit.log(user, "EXPORT_JSON", null, "");
    }

}
