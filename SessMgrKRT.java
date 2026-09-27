package com.school.util;

import com.school.model.User;

public class SessMgrKRT{
    private static User currentUser;

    public static void login(User user) { currentUser = user; }
    public static void logout() { currentUser = null; }
    public static User getCurrentUser() { return currentUser; }
    public static boolean isLoggedIn() { return currentUser != null; }
    public static boolean isAdmin() { return currentUser != null && currentUser.getRole() == User.RoleKrt.ADMIN; }
}
