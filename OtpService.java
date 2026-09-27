package com.school.service;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 6-digit OTP for password reset. Defaults (user chose Custom but gave no
 * numbers, so these apply until you ask to change them):
 *   6 digits, 10 min expiry, 5 max attempts, 60 s resend cooldown.
 * Codes are stored in the password_resets table.
 */
public class OtpService {

    public static final int OTP_DIGITS = 6;
    public static final long EXPIRY_MILLIS = 10L * 60 * 1000;
    public static final int MAX_ATTEMPTS = 5;
    public static final long RESEND_COOLDOWN_MILLIS = 60L * 1000;

    private final SecureRandom random = new SecureRandom();
    private final UserService userService = new UserService();

    public record RequestResult(boolean ok, String message, boolean demoMode, String demoOtp) {}
    public record VerifyResult(boolean ok, String message) {}

    public RequestResult requestOtp(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        if (email.isEmpty() || !email.contains("@")) {
            return new RequestResult(false, "Enter a valid email address.", false, null);
        }
        if (userService.findByEmail(email).isEmpty()) {
            return new RequestResult(false, "No account found for that email.", false, null);
        }
        DatabaseStorage.initDrm();
        long now = System.currentTimeMillis();
        try (Connection c = DatabaseStorage.getConnectionDrm();
             PreparedStatement ps = c.prepareStatement("SELECT last_sent_at FROM password_resets WHERE email=?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long wait = RESEND_COOLDOWN_MILLIS - (now - rs.getLong(1));
                    if (wait > 0) {
                        return new RequestResult(false,
                                "Please wait " + ((wait + 999) / 1000) + "s before resending.", false, null);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new RequestResult(false, "Database unavailable. Try again later.", false, null);
        }

        String otp = String.format("%06d", random.nextInt(1_000_000));
        long expiresAt = now + EXPIRY_MILLIS;
        try (Connection c = DatabaseStorage.getConnectionDrm();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO password_resets (email, otp, expires_at, attempts, last_sent_at) VALUES (?,?,?,?,?) "
                     + "ON CONFLICT (email) DO UPDATE SET otp=EXCLUDED.otp, expires_at=EXCLUDED.expires_at, "
                     + "attempts=0, last_sent_at=EXCLUDED.last_sent_at")) {
            ps.setString(1, email);
            ps.setString(2, otp);
            ps.setLong(3, expiresAt);
            ps.setInt(4, 0);
            ps.setLong(5, now);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            return new RequestResult(false, "Database unavailable. Try again later.", false, null);
        }

        if (EmailService.isConfigured()) {
            try {
                EmailService.sendOtp(email, otp, EXPIRY_MILLIS / 60000);
                return new RequestResult(true, "Code sent to " + email + ". Check your inbox.", false, null);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("[OtpService] SMTP send failed, demo fallback. OTP for " + email + ": " + otp);
                return new RequestResult(true,
                        "Email failed (" + e.getMessage() + "). Demo code: " + otp, true, otp);
            }
        }
        System.out.println("[OtpService] SMTP not configured. Demo OTP for " + email + ": " + otp);
        return new RequestResult(true,
                "Email not configured (demo mode). Your code is: " + otp, true, otp);
    }

    public VerifyResult verifyOtp(String rawEmail, String code) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        String otp = code == null ? "" : code.trim();
        if (otp.isEmpty()) return new VerifyResult(false, "Enter the 6-digit code.");
        try (Connection c = DatabaseStorage.getConnectionDrm();
             PreparedStatement ps = c.prepareStatement("SELECT otp, expires_at, attempts FROM password_resets WHERE email=?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new VerifyResult(false, "Request a code first.");
                String expected = rs.getString(1);
                long expiresAt = rs.getLong(2);
                int attempts = rs.getInt(3);
                long now = System.currentTimeMillis();
                if (attempts >= MAX_ATTEMPTS) {
                    clear(email);
                    return new VerifyResult(false, "Too many attempts. Request a new code.");
                }
                if (now > expiresAt) {
                    clear(email);
                    return new VerifyResult(false, "Code expired. Request a new one.");
                }
                if (!expected.equals(otp)) {
                    incrementAttempts(email, attempts + 1);
                    int left = MAX_ATTEMPTS - (attempts + 1);
                    return new VerifyResult(false, "Wrong code. " + left + " attempt(s) left.");
                }
                clear(email);
                return new VerifyResult(true, "Code verified.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new VerifyResult(false, "Database unavailable. Try again later.");
        }
    }

    public void clear(String email) {
        try (Connection c = DatabaseStorage.getConnectionDrm();
             PreparedStatement ps = c.prepareStatement("DELETE FROM password_resets WHERE email=?")) {
            ps.setString(1, email == null ? "" : email.trim().toLowerCase());
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void incrementAttempts(String email, int attempts) {
        try (Connection c = DatabaseStorage.getConnectionDrm();
             PreparedStatement ps = c.prepareStatement("UPDATE password_resets SET attempts=? WHERE email=?")) {
            ps.setInt(1, attempts);
            ps.setString(2, email);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }
}
