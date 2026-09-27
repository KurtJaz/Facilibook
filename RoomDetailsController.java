package com.school.controller; //address to kung saanong package ang file na ito

import java.util.Comparator;
import java.util.List;

import com.school.model.Booking;
import com.school.model.FacilityKRT;
import com.school.service.BookServ;
import com.school.util.Navigator;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RoomDetailsController {

    private final BookServ bookingService = new BookServ();

    @FXML private Label roomNameLabel;
    @FXML private Label roomLocationLabel;
    @FXML private Button backBtn;
    @FXML private FlowPane cardGrid;
    @FXML private Label emptyLabel;

    private FacilityKRT facility;

    @FXML
    public void initialize() {
        backBtn.setOnAction(e -> {
            Stage stage = (Stage) backBtn.getScene().getWindow();
            Navigator.go(stage, "availability");
        });
    }

    public void setFacility(FacilityKRT facility) {
        this.facility = facility;
        if (facility == null) return;
        roomNameLabel.setText(facility.getName());
        roomLocationLabel.setText(facility.getLocation());
        refresh();
    }

    private void refresh() {
        if (facility == null || cardGrid == null) return;
        cardGrid.getChildren().clear();

        List<Booking> bookings = bookingService.getByFacility(facility.getId()).stream()
                .filter(b -> b.getStatus() != Booking.Status.CANCELLED)
                .sorted(Comparator.comparing(Booking::getDate).thenComparing(Booking::getStartTime))
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
        Label facilityLabel = new Label(booking.getFacilityName());
        facilityLabel.getStyleClass().add("booking-card-facility");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label statusChip = new Label(prettify(booking.getStatus().name()));
        statusChip.getStyleClass().addAll("status-chip", "status-chip-" + booking.getStatus().name().toLowerCase());
        header.getChildren().addAll(facilityLabel, spacer, statusChip);

        VBox rows = new VBox(6,
                row("Name", booking.getUserName()),
                row("Time Start", to12Hour(booking.getStartTime())),
                row("Time End", to12Hour(booking.getEndTime())),
                row("Date", booking.getDate())
        );

        card.getChildren().addAll(header, rows);
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