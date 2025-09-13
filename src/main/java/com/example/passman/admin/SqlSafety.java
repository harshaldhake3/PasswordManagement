package com.example.passman.admin;

import java.util.Locale;
import java.util.regex.Pattern;

public final class SqlSafety {
    private static final Pattern FORBIDDEN = Pattern.compile("(?i)\b(INSERT|UPDATE|DELETE|MERGE|ALTER|DROP|TRUNCATE|CREATE|GRANT|REVOKE)\b");
    private static final Pattern SELECT = Pattern.compile("(?i)^\s*SELECT\b.*");

    public static boolean isSelectOnly(String q) {
        if (q == null) return false;
        String s = q.trim();
        if (!SELECT.matcher(s).find()) return false;
        return !FORBIDDEN.matcher(s).find();
    }

    public static String enforceLimit(String q, int max) {
        String s = q.trim();
        // naive limiter for H2/MySQL compatible
        if (s.toLowerCase(Locale.ROOT).contains(" limit ")) return s;
        return s + " LIMIT " + max;
    }
}
