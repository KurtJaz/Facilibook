package com.school.controller;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.school.model.Booking;
import com.school.model.User;
import com.school.service.BookServ;
import com.school.util.SessMgrKRT;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class ClassScheduleController {

    private final BookServ bookingService = new BookServ();

    @FXML private FlowPane cardGrid;
    @FXML private Label emptyLabel;
    @FXML private AnchorPane scheduleDatePickerTemplate;
    private DatePicker datePicker;
    @FXML private TextField searchField;

    @FXML
    public void initialize() {
        datePicker = (DatePicker) scheduleDatePickerTemplate.lookup("#datePicker");
        datePicker.valueProperty().addListener((o, old, val) -> refresh());
        searchField.textProperty().addListener((o, old, val) -> refresh());
        refresh();
    }

    private void refresh() {
        cardGrid.getChildren().clear();
        User user = SessMgrKRT.getCurrentUser();
        if (user == null) return;

        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String dateStr = datePicker.getValue() == null ? null
                : datePicker.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);

        List<Booking> entries = bookingService.getByUser(user.getId()).stream()
                .filter(b -> b.getStatus() == Booking.Status.APPROVED)
                .filter(b -> dateStr == null || dateStr.equals(b.getDate()))
                .filter(b -> query.isEmpty() || b.getFacilityName().toLowerCase().contains(query)
                        || b.getPurpose() != null && b.getPurpose().toLowerCase().contains(query))
                .toList();

        boolean empty = entries.isEmpty();
        emptyLabel.setVisible(empty);
        emptyLabel.setManaged(empty);

        for (Booking b : entries) {
            cardGrid.getChildren().add(buildCard(b));
        }
    }

    private VBox buildCard(Booking b) {
        VBox card = new VBox(2);
        card.getStyleClass().add("schedule-card");
        Label room = new Label(b.getFacilityName());
        room.getStyleClass().add("schedule-card-room");
        Label time = new Label(to12Hour(b.getStartTime()) + " - " + to12Hour(b.getEndTime()));
        time.getStyleClass().add("schedule-card-time");
        card.getChildren().addAll(room, time);
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
