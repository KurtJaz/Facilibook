package com.school.controller;

import java.util.List;

import com.school.model.Booking;
import com.school.model.User;
import com.school.service.BookServ;
import com.school.util.Navigator;
import com.school.util.SessMgrKRT;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DashboardController {

    private final BookServ bookingService = new BookServ();

    @FXML private Label sidebarNameLabel;
    @FXML private Label sidebarIdLabel;
    @FXML private Label userChipName;
    @FXML private Label userChipId;
    @FXML private Label welcomeNameLabel;
    @FXML private Label welcomeMetaLabel;
    @FXML private Label statusSectionLabel;
    @FXML private VBox notificationsContainer;
    @FXML private Button bookFacilityLink;
    @FXML private Button seeSchedulesLink;
    @FXML private Button bookingHistoryLink;

    @FXML
    public void initialize() {
        
        if (bookFacilityLink != null)
            bookFacilityLink.setOnAction(e -> go(bookFacilityLink, "book-facility"));
        if (seeSchedulesLink != null)
            seeSchedulesLink.setOnAction(e -> go(seeSchedulesLink, "class-schedule"));
        if (bookingHistoryLink != null)
            bookingHistoryLink.setOnAction(e -> go(bookingHistoryLink,
                    SessMgrKRT.isAdmin() ? "all-bookings" : "cancel-booking"));

        User user = SessMgrKRT.getCurrentUser();
        if (user == null || notificationsContainer == null) return;

        List<Booking> bookings = bookingService.getByUser(user.getId());
        boolean any = !bookings.isEmpty();
        statusSectionLabel.setVisible(any);
        statusSectionLabel.setManaged(any);
        notificationsContainer.setVisible(any);
        notificationsContainer.setManaged(any);
        if (!any) return;

        notificationsContainer.getChildren().clear();
        for (Booking booking : bookings) {
            notificationsContainer.getChildren().add(buildStatusBar(booking));
            if (booking.getStatus() == Booking.Status.APPROVED) {
                notificationsContainer.getChildren().add(buildStatRow(booking));
            }
        }
    }

    private HBox buildStatusBar(Booking booking) {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("status-bar");
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        switch (booking.getStatus()) {
            case APPROVED -> bar.getStyleClass().add("status-bar-approved");
            case REJECTED, CANCELLED -> bar.getStyleClass().add("status-bar-rejected");
            default -> bar.getStyleClass().add("status-bar-pending");
        }

        Label tag = new Label("FACILITY");
        tag.getStyleClass().add("status-tag");

        Label msg = new Label(statusMessage(booking));
        msg.getStyleClass().add("status-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label date = new Label(booking.getDate());
        date.getStyleClass().add("status-date");

        bar.getChildren().addAll(tag, msg, spacer, date);
        return bar;
    }

    private void go(Button source, String screen) {
        Stage stage = (Stage) source.getScene().getWindow();
        Navigator.go(stage, screen);
    }

    private String statusMessage(Booking booking) {
        return switch (booking.getStatus()) {
            case APPROVED -> "Your booking for " + booking.getFacilityName() + " has been approved";
            case REJECTED -> "Your booking for " + booking.getFacilityName() + " was rejected";
            case CANCELLED -> "Your booking for " + booking.getFacilityName() + " was cancelled";
            case PENDING -> "Your booking for " + booking.getFacilityName() + " is pending approval";
        };
    }

    private GridPane buildStatRow(Booking booking) {
        GridPane row = new GridPane();
        row.getStyleClass().add("cards-row");
        row.setHgap(14);
        row.setVgap(14);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            col.setHgrow(Priority.ALWAYS);
            row.getColumnConstraints().add(col);
        }

        row.add(statCard("Room Booked", booking.getFacilityName(), null), 0, 0);
        row.add(statCard("Scheduled Usage",
                to12Hour(booking.getStartTime()) + " - " + to12Hour(booking.getEndTime()),
                "stat-value-amber"), 1, 0);
        row.add(statCard("End Time", to12Hour(booking.getEndTime()), null), 2, 0);
        row.add(statCard("Booking Date", booking.getDate(), "stat-value-blue"), 3, 0);
        return row;
    }

    private VBox statCard(String head, String value, String valueStyle) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        Label h = new Label(head);
        h.getStyleClass().add("stat-head");
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        if (valueStyle != null) v.getStyleClass().add(valueStyle);
        v.setWrapText(true);
        card.getChildren().addAll(h, v);
        return card;
    }

    private String to12Hour(String hhmm) {
        try {
            int h = Integer.parseInt(hhmm.substring(0, 2));
            String m = hhmm.substring(3, 5);
            int hour = h == 0 ? 12 : h > 12 ? h - 12 : h;
            String suffix = h < 12 ? "AM" : "PM";
            return String.format("%02d:%s %s", hour, m, suffix);
        } catch (Exception e) {
            return hhmm;
        }
    }
}
