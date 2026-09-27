package com.school.util;

import com.school.model.User;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

/** Central screen switcher: loads FXML views and wires the shared sidebar. */
public final class Navigator {

    private static final java.util.Set<String> ADMIN_ONLY_SCREENS =
            java.util.Set.of("pending-bookings", "manage-facilities", "all-bookings");

    private Navigator() {}

    public static void go(Stage stage, String screen) {
        go(stage, screen, null);
    }

    /**
     * Same as go(stage, screen), but controllerConfigurator is invoked with the
     * target screen's controller right after its FXML loads, before the screen
     * is shown. Use this to hand data to the next screen, e.g.:
     *
     *   Navigator.go(stage, "room-details", controller ->
     *       ((RoomDetailsController) controller).setFacility(selectedFacility));
     */
    public static void go(Stage stage, String screen, Consumer<Object> controllerConfigurator) {
        if (!SessMgrKRT.isAdmin() && ADMIN_ONLY_SCREENS.contains(screen)) {
            screen = "dashboard";
        }
        // Auth + loader screens swap instantly — everything else goes through
        // the spinning loading screen so every navigation has a loading state.
        if ("loading-screen".equals(screen) || "login".equals(screen) || "register".equals(screen)) {
            setScreen(stage, screen, controllerConfigurator);
            return;
        }
        try {
            setScreen(stage, "loading-screen", null);
        } catch (RuntimeException ignored) {
            // Loader itself failed — fall through and try the target directly.
        }
        final String target = screen;
        showAfterLoading(stage, target, controllerConfigurator);
    }

    private static void showAfterLoading(Stage stage, String target, Consumer<Object> controllerConfigurator) {
        if (!stage.isShowing()) {
            setScreen(stage, target, controllerConfigurator);
            return;
        }

        AnimationTimer timer = new AnimationTimer() {
            private long startedAt;
            private int pulseCount;

            @Override
            public void handle(long now) {
                if (startedAt == 0) startedAt = now;
                pulseCount++;
                if (pulseCount < 3 || now - startedAt < 500_000_000L) return;
                stop();
                setScreen(stage, target, controllerConfigurator);
            }
        };
        timer.start();
    }

    /** Load an FXML screen and swap it in immediately (no loading transition). */
    private static void setScreen(Stage stage, String screen, Consumer<Object> controllerConfigurator) {
        if (!SessMgrKRT.isAdmin() && ADMIN_ONLY_SCREENS.contains(screen)) {
            screen = "dashboard";
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    Navigator.class.getResource("/com/school/view/" + screen + ".fxml"));
            Parent root = loader.load();
            if (controllerConfigurator != null) {
                controllerConfigurator.accept(loader.getController());
            }
            wireSidebar(root, stage);
            fillUserLabels(root);

            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            // Keep whatever window size is active (including maximized) so layouts fill it.
            if (stage.isMaximized()) return;
            if ("login".equals(screen) || "register".equals(screen)) {
                stage.setMinWidth(0);
                stage.setMinHeight(0);
                if (stage.getWidth() < 900 || stage.getHeight() < 560) {
                    stage.setWidth(1100);
                    stage.setHeight(700);
                }
            } else {
                stage.setMinWidth(1000);
                stage.setMinHeight(620);
                if (stage.getWidth() < 1000 || stage.getHeight() < 620) {
                    stage.setWidth(1200);
                    stage.setHeight(800);
                }
            }
        } catch (IOException | RuntimeException ex) {
            throw new RuntimeException("Couldn't load screen: " + screen, ex);
        }
    }

    /** Wire sidebar nav buttons found under {@code root} to their FXML screens. */
    private static void wireSidebar(Parent root, Stage stage) {
        bind(root, "#navDashboardBtn", stage, "dashboard");
        bind(root, "#navScheduleBtn", stage, "class-schedule");
        bind(root, "#navAvailabilityBtn", stage, "availability");
        bind(root, "#navBookBtn", stage, "book-facility");
        bind(root, "#navBookingsBtn", stage, "pending-bookings");
        bind(root, "#navCancelBtn", stage, "cancel-booking");
        bind(root, "#navManageFacilitiesBtn", stage, "manage-facilities");
        bind(root, "#navAllBookingsBtn", stage, "all-bookings");
        bind(root, "#navProfileBtn", stage, "my-profile");

        Button logout = find(root, "#navLogoutBtn");
        if (logout != null) {
            logout.setOnAction(e -> {
                SessMgrKRT.logout();
                go(stage, "login");
            });
        }

        boolean admin = SessMgrKRT.isAdmin();
        setVisible(root, "#navBookingsBtn", admin);
        setVisible(root, "#navManageFacilitiesBtn", admin);
        setVisible(root, "#navAllBookingsBtn", admin);
    }

    /** Replace FXML placeholder user text with the logged-in user from the session. */
    private static void fillUserLabels(Parent root) {
        User user = SessMgrKRT.getCurrentUser();
        if (user == null) return;

        setText(root, "#sidebarNameLabel", user.getName());
        setText(root, "#userChipName", user.getName());
        setText(root, "#welcomeNameLabel", user.getName());

        // User has no student-ID field yet — show role instead of the hardcoded demo ID.
        String detail = user.getRole().toString();
        setText(root, "#sidebarIdLabel", detail);
        setText(root, "#userChipId", detail);
        setText(root, "#welcomeMetaLabel", user.getEmail() + "  ·  " + detail);
    }

    private static void setText(Parent root, String selector, String value) {
        if (root.lookup(selector) instanceof Label label) label.setText(value);
    }

    private static void bind(Parent root, String selector, Stage stage, String screen) {
        Button b = find(root, selector);
        if (b != null) b.setOnAction(e -> go(stage, screen));
    }

    private static Button find(Parent root, String selector) {
        return (Button) root.lookup(selector);
    }

    private static void setVisible(Parent root, String selector, boolean visible) {
        Button b = find(root, selector);
        if (b != null) {
            b.setVisible(visible);
            b.setManaged(visible);
        }
    }
}