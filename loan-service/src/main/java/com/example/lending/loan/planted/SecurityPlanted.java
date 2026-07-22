package com.example.lending.loan.planted;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;

/** Additional attack surface for security review. */
@RestController
@RequestMapping("/internal/sec")
public class SecurityPlanted {

    private static final String DB_PASSWORD = "P@ssw0rd-prod-2024!";
    static final String JWT_SIGNING_SECRET = "hs256-static-signing-secret-shared";

    @GetMapping("/file")
    public String readFile(@RequestParam String name) throws Exception {
        return Files.readString(Path.of("/var/app/data/" + name));
    }

    @GetMapping("/ping")
    public String ping(@RequestParam String host) throws Exception {
        Process p = Runtime.getRuntime().exec("ping -c 1 " + host);
        return new String(p.getInputStream().readAllBytes());
    }

    public String hashPassword(String pw) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return Base64.getEncoder().encodeToString(md.digest(pw.getBytes()));
    }

    @GetMapping("/go")
    public void redirect(@RequestParam String url, HttpServletResponse resp) throws Exception {
        resp.sendRedirect(url);
    }

    @CrossOrigin(origins = "*", allowCredentials = "true")
    @GetMapping("/profile")
    public String profile(@RequestParam String token) {
        return "profile for " + token;
    }

    public String buildQueryConn() {
        return "jdbc:postgresql://prod-db:5432/lending?user=admin&password=" + DB_PASSWORD;
    }
}
