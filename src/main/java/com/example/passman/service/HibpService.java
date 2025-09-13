package com.example.passman.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class HibpService {
    private final HttpClient http = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .build();

    public boolean isPasswordPwned(String password) {
        try {
            String sha1 = sha1Hex(password).toUpperCase(Locale.ROOT);
            String prefix = sha1.substring(0, 5);
            String suffix = sha1.substring(5);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.pwnedpasswords.com/range/" + prefix))
                .header("Add-Padding", "true")
                .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return false; // fail open
            for (String line : res.body().split("\r?\n")) {
                String[] parts = line.split(":");
                if (parts.length >= 2 && parts[0].equalsIgnoreCase(suffix)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false; // fail open
        }
    }

    private static String sha1Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
