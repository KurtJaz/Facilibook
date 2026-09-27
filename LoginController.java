package com.school.controller;

import java.util.Optional;

import com.school.model.User;
import com.school.service.DatabaseStorage;
import com.school.service.OtpService;
import com.school.service.UserService;
import com.school.util.Navigator;
import com.school.util.SessMgrKRT;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginController {

    private final UserService userService = new UserService();
    private final OtpService otpService = new OtpService();

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberCheck;
    @FXML private Hyperlink forgotLink;
    @FXML private Label errorLabel;
    @FXML private Button loginBtn;
    @FXML private Button goSignupBtn;

    @FXML private VBox resetBox;
    @FXML private TextField resetEmailField;
    @FXML private Button sendCodeBtn;
    @FXML private VBox resetStep2Box;
    @FXML private TextField otpField;
    @FXML private Button verifyOtpBtn;
    @FXML private Hyperlink resendLink;
    @FXML private VBox resetStep3Box;
    @FXML private PasswordField newPassField;
    @FXML private PasswordField confirmPassField;
    @FXML private Button savePasswordBtn;
    @FXML private Label resetStatusLabel;

    @FXML private VBox loadingOverlay;
    @FXML private Label loadingMsg;
    @FXML private BorderPane contentShell;

    private boolean resetOpen = false;
    private boolean otpVerified = false;
    private boolean busy = false;
    private Task<Optional<User>> loginTask;
    private PauseTransition loginWatchdog;

    @FXML
    public void initialize() {
        loginBtn.setOnAction(e -> attemptLogin());
        goSignupBtn.setOnAction(e -> goToRegister());
        forgotLink.setOnAction(e -> toggleReset());
        passwordField.setOnAction(e -> attemptLogin());

        sendCodeBtn.setOnAction(e -> sendCode());
        verifyOtpBtn.setOnAction(e -> verifyCode());
        resendLink.setOnAction(e -> sendCode());
        savePasswordBtn.setOnAction(e -> saveNewPassword());

        PauseTransition dbCheck = new PauseTransition(Duration.seconds(4));
        dbCheck.setOnFinished(e -> {
            if (!DatabaseStorage.isInitialized() && DatabaseStorage.getInitError() != null) {
                showError("Database: " + DatabaseStorage.getInitError());
            }
        });
        dbCheck.play();
    }

    private void attemptLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Email and password are required.");
            return;
        }
        if (busy) return;
        busy = true;
        hideError();
        loginBtn.setDisable(true);
        showLoading("Logging in…");

        Task<Optional<User>> task = new Task<>() {
            @Override
            protected Optional<User> call() {
                return userService.authenticate(email, password);
            }
        };
        task.setOnSucceeded(e -> {
            finishLoading();
            Optional<User> user = task.getValue();
            if (user.isEmpty()) {
                showError("Incorrect email or password, please try again");
                return;
            }
            SessMgrKRT.login(user.get());
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            Navigator.go(stage, "dashboard");
        });
        task.setOnFailed(e -> {
            finishLoading();
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
            String detail = DatabaseStorage.getInitError();
            showError("Could not connect to the server. "
                    + (detail == null ? "Please try again." : detail));
        });
        loginTask = task;
        new Thread(task).start();
    }

    private void finishLoading() {
        hideLoading();
        busy = false;
        loginBtn.setDisable(false);
    }

    private void showLoading(String message) {
        loadingMsg.setText(message);
        // Blur the form behind the overlay; the overlay itself stays sharp.
        if (contentShell != null) contentShell.setEffect(new GaussianBlur(12));
        loadingOverlay.setVisible(true);
        loadingOverlay.setManaged(true);
        // Watchdog: the DB handshake can stall past its socket timeouts
        // (paused Supabase project, captive portal, VPN). Never trap the pill.
        if (loginWatchdog != null) loginWatchdog.stop();
        loginWatchdog = new PauseTransition(Duration.seconds(45));
        loginWatchdog.setOnFinished(e -> {
            if (!busy) return;
            if (loginTask != null) loginTask.cancel(true);
            finishLoading();
            showError("Connection timed out. Check your internet connection and "
                    + "that your Supabase project is running, then try again.");
        });
        loginWatchdog.play();
    }

    private void hideLoading() {
        if (loginWatchdog != null) {
            loginWatchdog.stop();
            loginWatchdog = null;
        }
        loginTask = null;
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);
        if (contentShell != null) contentShell.setEffect(null);
    }

    private void goToRegister() {
        Stage stage = (Stage) goSignupBtn.getScene().getWindow();
        Navigator.go(stage, "register");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }


    private void toggleReset() {
        resetOpen = !resetOpen;
        resetBox.setVisible(resetOpen);
        resetBox.setManaged(resetOpen);
        forgotLink.setText(resetOpen ? "Back to Login" : "Forgot Password?");
        if (resetOpen) {
            hideError();
            if (resetEmailField.getText().isBlank() && !emailField.getText().isBlank()) {
                resetEmailField.setText(emailField.getText().trim());
            }
        } else {
            resetStep2Box.setVisible(false);
            resetStep2Box.setManaged(false);
            resetStep3Box.setVisible(false);
            resetStep3Box.setManaged(false);
            hideResetStatus();
            otpVerified = false;
        }
    }

    private void sendCode() {
        otpVerified = false;
        resetStep3Box.setVisible(false);
        resetStep3Box.setManaged(false);
        OtpService.RequestResult r = otpService.requestOtp(resetEmailField.getText());
        showResetStatus(r.message(), !r.ok());
        if (r.ok()) {
            resetStep2Box.setVisible(true);
            resetStep2Box.setManaged(true);
            otpField.requestFocus();
        }
    }

    private void verifyCode() {
        OtpService.VerifyResult r = otpService.verifyOtp(resetEmailField.getText(), otpField.getText());
        showResetStatus(r.message(), !r.ok());
        if (r.ok()) {
            otpVerified = true;
            resetStep3Box.setVisible(true);
            resetStep3Box.setManaged(true);
            newPassField.requestFocus();
        }
    }

    private void saveNewPassword() {
        if (!otpVerified) {
            showResetStatus("Verify the code first.", true);
            return;
        }
        String p1 = newPassField.getText();
        String p2 = confirmPassField.getText();
        if (p1 == null || p1.length() < 6) {
            showResetStatus("Password must be at least 6 characters.", true);
            return;
        }
        if (!p1.equals(p2)) {
            showResetStatus("Passwords do not match.", true);
            return;
        }
        boolean ok = userService.updatePasswordByEmail(resetEmailField.getText().trim(), p1);
        if (!ok) {
            showResetStatus("Could not update password. Try again.", true);
            return;
        }
        otpService.clear(resetEmailField.getText());
        showResetStatus("Password updated. You can log in now.", false);
        emailField.setText(resetEmailField.getText().trim());
        passwordField.clear();
        newPassField.clear();
        confirmPassField.clear();
        otpField.clear();
        otpVerified = false;
        resetStep2Box.setVisible(false);
        resetStep2Box.setManaged(false);
        resetStep3Box.setVisible(false);
        resetStep3Box.setManaged(false);
    }

    private void showResetStatus(String message, boolean isError) {
        resetStatusLabel.setText(message);
        resetStatusLabel.setVisible(true);
        resetStatusLabel.setManaged(true);
        resetStatusLabel.setStyle(isError ? "-fx-text-fill: #dc2626;" : "-fx-text-fill: #15803d;");
    }

    private void hideResetStatus() {
        resetStatusLabel.setVisible(false);
        resetStatusLabel.setManaged(false);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
