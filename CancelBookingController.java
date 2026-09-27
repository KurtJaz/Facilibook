package com.school.controller;

import com.school.model.Booking;
import com.school.model.User;
import com.school.service.BookServ;
import com.school.util.SessMgrKRT;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class CancelBookingController {

    private final BookServ bookingService = new BookServ();

    @FXML private FlowPane cardGrid;
    @FXML private Label emptyLabel;

    @FXML
    public void initialize() {
        refresh();
    }

    private void refresh() {
        cardGrid.getChildren().clear();
        User user = SessMgrKRT.getCurrentUser();
        if (user == null) return;

        // only pending/approved are "still upcoming" — cancelled/rejected have nothing left to cancel
        List<Booking> upcoming = bookingService.getByUser(user.getId()).stream()
                .filter(b -> b.getStatus() == Booking.Status.PENDING || b.getStatus() == Booking.Status.APPROVED)
                .toList();

        boolean empty = upcoming.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);

        for (Booking booking : upcoming) {
            cardGrid.getChildren().add(buildCard(booking, user));
        }
    }

    private VBox buildCard(Booking booking, User user) {
        VBox card = new VBox(8);
        card.getStyleClass().add("booking-card");

        HBox header = new HBox(8);
        Label facility = new Label(booking.getFacilityName());
        facility.getStyleClass().add("booking-card-facility");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label statusChip = new Label(prettify(booking.getStatus().name()));
        statusChip.getStyleClass().addAll("status-chip", "status-chip-" + booking.getStatus().name().toLowerCase());
        header.getChildren().addAll(facility, spacer, statusChip);

        VBox rows = new VBox(6,
                row("Name", booking.getUserName()),
                row("Time Start", to12Hour(booking.getStartTime())),
                row("Time End", to12Hour(booking.getEndTime())),
                row("Date", booking.getDate())
        );
        VBox.setMargin(rows, new Insets(0, 0, 14, 0));

        card.getChildren().addAll(header, rows);

        if (booking.getStatus() == Booking.Status.PENDING) {
            Button cancelBtn = new Button("Cancel Booking");
            cancelBtn.getStyleClass().add("btn-reject");
            cancelBtn.setMaxWidth(Double.MAX_VALUE);
            cancelBtn.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Cancel this booking request?", ButtonType.YES, ButtonType.NO);
                alert.showAndWait().ifPresent(r -> {
                    if (r == ButtonType.YES) {
                        bookingService.cancelBooking(booking.getId(), user.getId(), SessMgrKRT.isAdmin());
                        refresh();
                    }
                });
            });
            card.getChildren().add(cancelBtn);
        } else {
            // APPROVED: nothing to cancel here — it proceeds automatically as scheduled
            Label note = new Label("Approved — this booking proceeds automatically, no action needed.");
            note.getStyleClass().add("auto-note");
            note.setWrapText(true);
            card.getChildren().add(note);
        }

        return card;
    }

    private HBox row(String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("card-row-k");
        Label v = new Label(value);
        v.getStyleClass().add("card-row-v");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(k, spacer, v);
        row.getStyleClass().add("card-row");
        return row;
    }

    private String prettify(String enumName) {
        return enumName.charAt(0) + enumName.substring(1).toLowerCase();
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
