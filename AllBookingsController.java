package com.school.controller;
import com.school.model.Booking;
import com.school.service.BookServ;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.util.List;
public class AllBookingsController {

    private final BookServ bookingService = new BookServ();
    @FXML private FlowPane cardGrid;
    @FXML private Label emptyLabel;
    @FXML private ToggleButton filterAll, filterPending, filterApproved, filterRejected, filterCancelled;

    private final ToggleGroup filterG = new ToggleGroup();

    @FXML
    public void initialize() {
        for (ToggleButton b : List.of(filterAll, filterPending, filterApproved, filterRejected, filterCancelled)) {
            b.setToggleGroup(filterG);
        }
        filterAll.setSelected(true);
        filterG.selectedToggleProperty().addListener((o, old, val) -> {
            if (val == null) filterAll.setSelected(true); // prevent deselecting all tabs
            else refresh();
        });
        refresh();
    }

    private Booking.Status selectedStatus() {
        Toggle t = filterG.getSelectedToggle();
        if (t == filterPending) return Booking.Status.PENDING;
        if (t == filterApproved) return Booking.Status.APPROVED;
        if (t == filterRejected) return Booking.Status.REJECTED;
        if (t == filterCancelled) return Booking.Status.CANCELLED;
        return null; // "All"
    }

    private void refresh() {
        cardGrid.getChildren().clear();
        Booking.Status filter = selectedStatus();
        List<Booking> bookings = bookingService.getAll().stream()
                .filter(b -> filter == null || b.getStatus() == filter)
                .toList();

        boolean empty = bookings.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);

        for (Booking booking : bookings) {
            cardGrid.getChildren().add(buildCard(booking));
        }
    }

    private VBox buildCard(Booking booking) {
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
            Button acceptBtn = new Button("Accept");
            acceptBtn.getStyleClass().add("btn-accept");
            acceptBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(acceptBtn, Priority.ALWAYS);

            Button rejectBtn = new Button("Reject");
            rejectBtn.getStyleClass().add("btn-reject");
            rejectBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(rejectBtn, Priority.ALWAYS);

            acceptBtn.setOnAction(e -> { bookingService.updateStatus(booking.getId(), Booking.Status.APPROVED); refresh(); });
            rejectBtn.setOnAction(e -> { bookingService.updateStatus(booking.getId(), Booking.Status.REJECTED); refresh(); });

            card.getChildren().add(new HBox(10, acceptBtn, rejectBtn));
        }

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-delete");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Permanently delete this booking?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait().ifPresent(r -> {
                if (r == ButtonType.YES) { bookingService.deleteBooking(booking.getId()); refresh(); }
            });
        });
        card.getChildren().add(deleteBtn);

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
