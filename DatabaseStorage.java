package com.school.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DatabaseStorage {

    /** Values from a local `.env` file (working directory), if present. */
    private static final Map<String, String> DOTENV = loadDotenv();

    private static boolean initializedDrm = false;
    private static String lastInitError = null;

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /** Real environment variables win; `.env` file is the fallback. */
    private static String env(String name) {
        String v = System.getenv(name);
        if (v != null && !v.isBlank()) return v;
        v = DOTENV.get(name);
        return (v == null || v.isBlank()) ? null : v;
    }

    private static Map<String, String> loadDotenv() {
        Map<String, String> map = new HashMap<>();
        for (String candidate : new String[] { ".env", "supabase/.env" }) {
            if (!Files.isRegularFile(Paths.get(candidate))) continue;
            try {
                for (String line : Files.readAllLines(Paths.get(candidate))) {
                    String t = line.trim();
                    if (t.isEmpty() || t.startsWith("#")) continue;
                    int eq = t.indexOf('=');
                    if (eq <= 0) continue;
                    String k = t.substring(0, eq).trim();
                    String v = t.substring(eq + 1).trim();
                    if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
                        v = v.substring(1, v.length() - 1);
                    } else if (v.length() >= 2 && v.startsWith("'") && v.endsWith("'")) {
                        v = v.substring(1, v.length() - 1);
                    }
                    map.putIfAbsent(k, v);
                }
                break; // first file found wins
            } catch (IOException ignored) {
                // Unreadable .env — behave as if it isn't there.
            }
        }
        return map;
    }

    private static Properties buildConnProps() {
        Properties p = new Properties();
        p.setProperty("user", env("SUPABASE_DB_USER"));
        p.setProperty("password", env("SUPABASE_DB_PASS"));
        p.setProperty("connectTimeout", "5");
        p.setProperty("socketTimeout", "15");
        p.setProperty("sslmode", "require");
        return p;
    }

    public static Connection getConnectionDrm() throws SQLException {
        String url = env("SUPABASE_DB_URL");
        if (url == null || env("SUPABASE_DB_USER") == null || env("SUPABASE_DB_PASS") == null) {
            throw new SQLException(
                "Missing Supabase connection settings. Set the environment variables "
                + "SUPABASE_DB_URL, SUPABASE_DB_USER, and SUPABASE_DB_PASS, "
                + "or put them in a local `.env` file."
            );
        }
        return DriverManager.getConnection(url, buildConnProps());
    }

    public static synchronized void initDrm() {
        if (initializedDrm) return;
        if (env("SUPABASE_DB_URL") == null || env("SUPABASE_DB_USER") == null || env("SUPABASE_DB_PASS") == null) {
            lastInitError = "Missing connection settings: SUPABASE_DB_URL, SUPABASE_DB_USER, SUPABASE_DB_PASS (env vars or local `.env`)";
            System.err.println("[DatabaseStorage] " + lastInitError);
            return;
        }
        try (Connection c = getConnectionDrm(); Statement st = c.createStatement()) {
            st.execute("SELECT 1");
            createTablesIfNotExistsDrm();
            initializedDrm = true;
            lastInitError = null;
        } catch (Exception e) {
            lastInitError = e.getMessage();
            System.err.println("[DatabaseStorage] Init failed: " + lastInitError);
            e.printStackTrace();
        }
    }

    /** Runs table creation + seed on a daemon thread so the UI stays responsive. */
    public static void initAsync() {
        Thread t = new Thread(DatabaseStorage::initDrm, "database-init");
        t.setDaemon(true);
        t.start();
    }

    public static boolean isInitialized() {
        return initializedDrm;
    }

    public static String getInitError() {
        return lastInitError;
    }

    private static void createTablesIfNotExistsDrm() throws Exception {
        try (Connection c = getConnectionDrm(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, name TEXT NOT NULL, email TEXT UNIQUE NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL CHECK (role IN ('STUDENT','TEACHER','ADMIN')))");
            st.execute("CREATE TABLE IF NOT EXISTS facilities (id TEXT PRIMARY KEY, name TEXT NOT NULL, type TEXT NOT NULL CHECK (type IN ('CLASSROOM','LABORATORY','SPORTS','AUDITORIUM','LIBRARY','CONFERENCE')), location TEXT, capacity INTEGER NOT NULL, description TEXT, active BOOLEAN NOT NULL DEFAULT true)");
            st.execute("CREATE TABLE IF NOT EXISTS bookings (id TEXT PRIMARY KEY, user_id TEXT NOT NULL, user_name TEXT NOT NULL, facility_id TEXT NOT NULL, facility_name TEXT NOT NULL, date TEXT NOT NULL, start_time TEXT NOT NULL, end_time TEXT NOT NULL, purpose TEXT, status TEXT NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')), FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY(facility_id) REFERENCES facilities(id) ON DELETE CASCADE)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_bookings_user ON bookings(user_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_bookings_facility ON bookings(facility_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_bookings_date ON bookings(date)");
            st.execute("CREATE TABLE IF NOT EXISTS password_resets (email TEXT PRIMARY KEY, otp TEXT NOT NULL, expires_at BIGINT NOT NULL, attempts INT NOT NULL DEFAULT 0, last_sent_at BIGINT NOT NULL DEFAULT 0)");
            st.execute("INSERT INTO users (id, name, email, password, role) VALUES ('9a38f34f-6ec5-435d-9893-fe3703300307', 'Admin', 'admin@school.edu', 'admin123', 'ADMIN'), ('574be51b-70e6-4d60-bc3f-06da76cd65e8', 'John Teacher', 'teacher@school.edu', 'teacher123', 'TEACHER'), ('c207eceb-77f8-42d7-a645-a1ceba7bd6aa', 'Alice Student', 'student@school.edu', 'student123', 'STUDENT') ON CONFLICT (id) DO NOTHING");
        }
    }
}
