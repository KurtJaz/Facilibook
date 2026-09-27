package com.school.controller;

import com.school.model.Booking;
import com.school.model.FacilityKRT;
import com.school.service.BookServ;
import com.school.service.FacilservJRD;
import com.school.util.Navigator;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AvailabilityController {

    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final FacilservJRD facilityService = new FacilservJRD();
    private final BookServ bookingService = new BookServ();
    private int refreshRequestId;

    @FXML private ListView<AvailabilityRow> facilityListView;
    @FXML private AnchorPane availabilityDatePickerTemplate;
    private DatePicker datePicker;
    // NEW: fx:id="searchField" — add a TextField with this id to availability.fxml
    // (see the search-field snippet), placed near the date filter row.
    @FXML private TextField searchField;
    @FXML private Label availabilityStatusLabel;
    @FXML private Label sidebarNameLabel;
    @FXML private Label userChipName;

    /** Unfiltered rows for the currently selected date — searchField filters this in memory. */
    private List<AvailabilityRow> allRows = List.of();

    @FXML
    public void initialize() {
        datePicker = (DatePicker) availabilityDatePickerTemplate.lookup("#datePicker");
        facilityListView.setCellFactory(lv -> new FacilityCell());
        facilityListView.setOnMouseClicked(e -> {
            AvailabilityRow selected = facilityListView.getSelectionModel().getSelectedItem();
            if (selected != null) openDetails(selected.facility());
        });
        datePicker.setValue(LocalDate.now());
        datePicker.valueProperty().addListener((observable, previousDate, selectedDate) -> {
            if (selectedDate != null) refresh();
        });
        if (searchField != null) {
            searchField.textProperty().addListener((o, old, val) -> applyFilter(val));
        }
        refresh();
    }

    private void openDetails(FacilityKRT facility) {
        Stage stage = (Stage) facilityListView.getScene().getWindow();
        Navigator.go(stage, "room-details", controller -> {
            if (controller instanceof RoomDetailsController rdc) rdc.setFacility(facility);
        });
    }

    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ENGLISH);
        List<AvailabilityRow> filtered = q.isEmpty() ? allRows : allRows.stream()
                .filter(r -> (r.facility().getName() + " " + r.facility().getLocation())
                        .toLowerCase(Locale.ENGLISH).contains(q))
                .toList();
        facilityListView.getItems().setAll(filtered);
    }

    private void refresh() {
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) return;

        int requestId = ++refreshRequestId;
        String date = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        availabilityStatusLabel.setText("Loading availability...");
        facilityListView.setDisable(true);

        Task<List<AvailabilityRow>> loadTask = new Task<>() {
            @Override
            protected List<AvailabilityRow> call() {
                List<FacilityKRT> facilities = facilityService.getActive();
                Set<String> bookedFacilityIds = new HashSet<>();
                for (Booking booking : bookingService.getBlockingByDate(date)) {
                    bookedFacilityIds.add(booking.getFacilityId());
                }

                List<AvailabilityRow> rows = new ArrayList<>();
                for (FacilityKRT facility : facilities) {
                    rows.add(new AvailabilityRow(facility, bookedFacilityIds.contains(facility.getId())));
                }
                return rows;
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (requestId != refreshRequestId) return;
            allRows = loadTask.getValue();
            applyFilter(searchField == null ? null : searchField.getText());
            facilityListView.setDisable(false);
            availabilityStatusLabel.setText(allRows.isEmpty()
                    ? "No active facilities available."
                    : "Showing availability for " + selectedDate.format(DISPLAY_DATE_FORMATTER));
        });
        loadTask.setOnFailed(event -> {
            if (requestId != refreshRequestId) return;
            facilityListView.setDisable(false);
            availabilityStatusLabel.setText("Unable to load availability.");
        });

        Thread loader = new Thread(loadTask, "availability-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private class FacilityCell extends ListCell<AvailabilityRow> {
        @Override
        protected void updateItem(AvailabilityRow row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            FacilityKRT facility = row.facility();
            Label name = new Label(facility.getName());
            name.getStyleClass().add("facility-row-name");
            Label location = new Label(facility.getLocation());
            location.getStyleClass().add("facility-row-location");
            VBox nameBox = new VBox(2, name, location);

            boolean inUse = row.booked();
            Circle dot = new Circle(3.5);
            dot.getStyleClass().add(inUse ? "status-dot-booked" : "status-dot-available");
            Label statusLabel = new Label(inUse ? "Booked" : "Available");
            statusLabel.getStyleClass().add(inUse ? "status-pill-booked" : "status-pill-available");
            HBox statusBox = new HBox(6, dot, statusLabel);
            statusBox.setAlignment(Pos.CENTER_LEFT);

            Label chevron = new Label("\u203A");
            chevron.getStyleClass().add("chevron");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox facilityRow = new HBox(14, nameBox, spacer, statusBox, chevron);
            facilityRow.setAlignment(Pos.CENTER_LEFT);
            facilityRow.getStyleClass().add("facility-row");
            facilityRow.setPadding(new Insets(0));

            setGraphic(facilityRow);
            setText(null);
        }
    }

    private record AvailabilityRow(FacilityKRT facility, boolean booked) {}
}