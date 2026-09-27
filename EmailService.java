package com.school.service;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Sends OTP codes via SMTP. Credentials come ONLY from environment variables
 * so secrets are never committed:
 *   SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SMTP_FROM, SMTP_FROM_NAME
 * If not configured, callers should fall back to demo mode (log the code).
 */
public class EmailService {

    public static boolean isConfigured() {
        return env("SMTP_HOST") != null && env("SMTP_USER") != null && env("SMTP_PASS") != null;
    }

    public static void sendOtp(String toEmail, String otp, long expiryMinutes) throws MessagingException {
        String host = env("SMTP_HOST");
        String portStr = env("SMTP_PORT");
        String user = env("SMTP_USER");
        String pass = env("SMTP_PASS");
        String from = env("SMTP_FROM");
        String fromName = env("SMTP_FROM_NAME");
        if (host == null || user == null || pass == null) {
            throw new MessagingException("SMTP not configured (SMTP_HOST/SMTP_USER/SMTP_PASS missing)");
        }
        int port = 587;
        try { if (portStr != null) port = Integer.parseInt(portStr.trim()); } catch (NumberFormatException ignored) {}
        if (from == null || from.isBlank()) from = user;
        if (fromName == null || fromName.isBlank()) fromName = "Facilibook";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.connectiontimeout", "15000");
        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        final String smtpUser = user;
        final String smtpPass = pass.replace(" ", "");
        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPass);
            }
        });

        MimeMessage msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(from, fromName));
        } catch (Exception e) {
            msg.setFrom(new InternetAddress(from));
        }
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        msg.setSubject("Facilibook password reset code");
        msg.setText("Your Facilibook password reset code is:\n\n  " + otp + "\n\n"
                + "It expires in " + expiryMinutes + " minutes.\n"
                + "If you did not request this, ignore this email.");
        Transport.send(msg);
    }

    private static String env(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) return null;
        return v.trim();
    }
}
