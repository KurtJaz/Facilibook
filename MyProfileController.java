package com.school.controller;

import com.school.model.User;
import com.school.service.UserService;
import com.school.util.SessMgrKRT;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MyProfileController {

    private final UserService userService = new UserService();

    @FXML private Label nameLabel;
    @FXML private Label emailSubLabel;
    @FXML private Label emailValueLabel;
    @FXML private Label roleValueLabel;
    @FXML private Label passwordValueLabel;
    @FXML private Button editBtn;

    @FXML private Label sidebarNameLabel;
    @FXML private Label sidebarIdLabel;

    @FXML
    public void initialize() {
        editBtn.setOnAction(e -> openEditDialog());
        refresh();
    }

    private void refresh() {
        User user = SessMgrKRT.getCurrentUser();
        if (user == null) return;

        nameLabel.setText(user.getName());
        emailSubLabel.setText(user.getEmail());
        emailValueLabel.setText(user.getEmail());
        roleValueLabel.setText(user.getRole().toString());
        passwordValueLabel.setText("••••••••");

        if (sidebarNameLabel != null) sidebarNameLabel.setText(user.getName());
        if (sidebarIdLabel != null) sidebarIdLabel.setText("ID: " + user.getId().substring(0, Math.min(8, user.getId().length())));
    }

    private void openEditDialog() {
        User user = SessMgrKRT.getCurrentUser();
        if (user == null) return;

        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Edit Profile");

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(12);
        g.setPadding(new Insets(20));

        TextField nameField = new TextField(user.getName());
        nameField.getStyleClass().add("dialog-field");
        TextField emailField = new TextField(user.getEmail());
        emailField.getStyleClass().add("dialog-field");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Leave blank to keep current password");
        passField.getStyleClass().add("dialog-field");

        g.add(new Label("Name:"), 0, 0); g.add(nameField, 1, 0);
        g.add(new Label("Email:"), 0, 1); g.add(emailField, 1, 1);
        g.add(new Label("New Password:"), 0, 2); g.add(passField, 1, 2);

        Label msg = new Label();
        msg.setStyle("-fx-text-fill: #f0847d;");

        Button save = new Button("Save");
        Button cancel = new Button("Cancel");
        HBox btnRow = new HBox(10, save, cancel);

        save.setOnAction(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String newPass = passField.getText();

            if (name.isEmpty() || email.isEmpty()) { msg.setText("Name and email are required."); return; }
            if (!email.contains("@")) { msg.setText("Enter a valid email."); return; }

            boolean ok = userService.updateUser(user.getId(), name, email, newPass);
            if (ok) {
                user.setName(name);
                user.setEmail(email);
                if (!newPass.isBlank()) user.setPassword(newPass);
                refresh();
                dlg.close();
            } else {
                msg.setText("That email is already in use.");
            }
        });
        cancel.setOnAction(e -> dlg.close());

        VBox root = new VBox(14, g, msg, btnRow);
        root.setPadding(new Insets(6));
        dlg.setScene(new Scene(root, 380, 260));
        dlg.showAndWait();
    }
}
