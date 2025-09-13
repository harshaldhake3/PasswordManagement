package com.example.passman.admin;

public class AdminDTOs {
    public static class UserRow {
        public Long id;
        public String username;
        public String role;
        public UserRow(Long id, String username, String role) {
            this.id = id; this.username = username; this.role = role;
        }
    }
    public static class CredRow {
        public Long id;
        public String site;
        public String login;
        public String url;
        public String tags;
        public CredRow(Long id, String site, String login, String url, String tags) {
            this.id = id; this.site = site; this.login = login; this.url = url; this.tags = tags;
        }
    }
}
