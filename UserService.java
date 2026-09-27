package com.school.service;

import com.school.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserService {

    public UserService() {
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getString("id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        String roleStr = rs.getString("role");
        try { u.setRole(User.RoleKrt.valueOf(roleStr)); } catch (Exception e) { u.setRole(User.RoleKrt.STUDENT); }
        return u;
    }

    public Optional<User> authenticate(String email, String password) {
        String sql = "SELECT * FROM users WHERE LOWER(email)=LOWER(?) AND password=?";
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public boolean register(String name, String email, String password, User.RoleKrt role) {
        if (findByEmail(email).isPresent()) return false;
        String sql = "INSERT INTO users (id,name,email,password,role) VALUES (?,?,?,?,?)";
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, name);
            ps.setString(3, email);
            ps.setString(4, password);
            ps.setString(5, role.name());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        try (Connection c = DatabaseStorage.getConnectionDrm(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM users")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Optional<User> findById(String id) {
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(mapRow(rs)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE LOWER(email)=LOWER(?)")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return Optional.of(mapRow(rs)); }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public boolean updatePasswordByEmail(String email, String newPassword) {
        try (Connection c = DatabaseStorage.getConnectionDrm();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET password=? WHERE LOWER(email)=LOWER(?)")) {
            ps.setString(1, newPassword);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /**
     * Updates name/email, and password only when a non-blank newPassword is given.
     * Returns false if the new email is already taken by a different user.
     */
    public boolean updateUser(String id, String name, String email, String newPassword) {
        Optional<User> existing = findByEmail(email);
        if (existing.isPresent() && !existing.get().getId().equals(id)) return false;

        String sql = (newPassword == null || newPassword.isBlank())
                ? "UPDATE users SET name=?, email=? WHERE id=?"
                : "UPDATE users SET name=?, email=?, password=? WHERE id=?";
        try (Connection c = DatabaseStorage.getConnectionDrm(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            if (newPassword == null || newPassword.isBlank()) {
                ps.setString(3, id);
            } else {
                ps.setString(3, newPassword);
                ps.setString(4, id);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}

