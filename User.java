package com.school.model;

public class User {
    public enum RoleKrt { STUDENT, TEACHER, ADMIN }

    private String id;
    private String name;
    private String email;
    private String password;
    private RoleKrt role;

    public User() {}

    public User(String id, String name, String email, String password, RoleKrt role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public RoleKrt getRole() { return role; }
    public void setRole(RoleKrt role) { this.role = role; }

    @Override
    public String toString() {
        return name + " (" + role + ")";
    }
}
