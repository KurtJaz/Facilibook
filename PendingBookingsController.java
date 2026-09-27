package com.school.controller;

import com.school.model.Booking;
import com.school.service.BookServ;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class PendingBookingsController {

    private final BookServ bookingService = new BookServ();

    @FXML private FlowPane cardGrid;
    @FXML private Label emptyLabel;

    @FXML
    public void initialize() {
        refresh();
    }

    private void refresh() {
        cardGrid.getChildren().clear();
        List<Booking> pending = bookingService.getAll().stream()
                .filter(b -> b.getStatus() == Booking.Status.PENDING)
                .toList();

        boolean empty = pending.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);

        for (Booking booking : pending) {
            cardGrid.getChildren().add(buildCard(booking));
        }
    }

    private VBox buildCard(Booking booking) {
        VBox card = new VBox(8);
        card.getStyleClass().add("booking-card");

        Label facility = new Label(booking.getFacilityName());
        facility.getStyleClass().add("booking-card-facility");

        VBox rows = new VBox(
                row("Name", booking.getUserName()),
                row("Time Start", to12Hour(booking.getStartTime())),
                row("Time End", to12Hour(booking.getEndTime()))
        );
        rows.setSpacing(6);
        VBox.setMargin(rows, new javafx.geometry.Insets(0, 0, 14, 0));

        Button acceptBtn = new Button("Accept");
        acceptBtn.getStyleClass().add("btn-accept");
        acceptBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(acceptBtn, javafx.scene.layout.Priority.ALWAYS);

        Button rejectBtn = new Button("Reject");
        rejectBtn.getStyleClass().add("btn-reject");
        rejectBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(rejectBtn, javafx.scene.layout.Priority.ALWAYS);

        HBox btnRow = new HBox(10, acceptBtn, rejectBtn);

        acceptBtn.setOnAction(e -> decide(card, btnRow, booking, Booking.Status.APPROVED));
        rejectBtn.setOnAction(e -> decide(card, btnRow, booking, Booking.Status.REJECTED));

        card.getChildren().addAll(facility, rows, btnRow);
        return card;
    }

    private HBox row(String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("card-row-k");
        Label v = new Label(value);
        v.getStyleClass().add("card-row-v");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox row = new HBox(k, spacer, v);
        row.getStyleClass().add("card-row");
        return row;
    }

    private void decide(VBox card, HBox btnRow, Booking booking, Booking.Status outcome) {
        boolean ok = bookingService.updateStatus(booking.getId(), outcome);
        if (!ok) return;

        Label tag = new Label(outcome == Booking.Status.APPROVED ? "Approved" : "Rejected");
        tag.getStyleClass().addAll("decided-tag",
                outcome == Booking.Status.APPROVED ? "decided-approved" : "decided-rejected");
        tag.setMaxWidth(Double.MAX_VALUE);

        int idx = card.getChildren().indexOf(btnRow);
        if (idx >= 0) card.getChildren().set(idx, tag);
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
