package com.school.controller;
import com.school.model.User;
import com.school.service.UserService;
import com.school.util.Navigator;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    private final UserService userService = new UserService();

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<User.RoleKrt> roleBox;
    @FXML private Label errorLabel;
    @FXML private Button registerBtn;
    @FXML private Button goLoginBtn;

    @FXML
    public void initialize() {
        roleBox.setItems(FXCollections.observableArrayList(User.RoleKrt.STUDENT, User.RoleKrt.TEACHER));
        roleBox.setValue(User.RoleKrt.STUDENT);

        registerBtn.setOnAction(e -> attemptRegister());
        goLoginBtn.setOnAction(e -> goToLogin());
    }

    private void attemptRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        User.RoleKrt role = roleBox.getValue();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Name, email, and password are all required.");
            return;
        }
        if (!email.contains("@")) {
            showError("Enter a valid email.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords don't match.");
            return;
        }
        if (role == null) role = User.RoleKrt.STUDENT;

        boolean ok = userService.register(name, email, password, role);
        if (!ok) {
            showError("An account with that email already exists.");
            return;
        }

        goToLogin(); // account created — send them to log in
    }

    private void goToLogin() {
        Stage stage = (Stage) goLoginBtn.getScene().getWindow();
        Navigator.go(stage, "login");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
