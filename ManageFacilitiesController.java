package com.school.controller;

import com.school.model.FacilityKRT;
import com.school.service.FacilservJRD;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ManageFacilitiesController {

    private final FacilservJRD facilityService = new FacilservJRD();

    @FXML private TableView<FacilityKRT> facilityTable;
    @FXML private TableColumn<FacilityKRT, String> nameCol;
    @FXML private TableColumn<FacilityKRT, String> typeCol;
    @FXML private TableColumn<FacilityKRT, String> locationCol;
    @FXML private TableColumn<FacilityKRT, String> capacityCol;
    @FXML private TableColumn<FacilityKRT, String> activeCol;
    @FXML private TableColumn<FacilityKRT, Void> actionsCol;
    @FXML private Button addBtn;

    @FXML
    public void initialize() {
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType().toString()));
        locationCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLocation()));
        capacityCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCapacity())));
        activeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActive() ? "Yes" : "No"));
        activeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(value);
                getStyleClass().removeAll("active-yes", "active-no");
                getStyleClass().add(value.equals("Yes") ? "active-yes" : "active-no");
            }
        });

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button toggleBtn = new Button();
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(6, editBtn, toggleBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("action-btn");
                toggleBtn.getStyleClass().add("action-btn");
                deleteBtn.getStyleClass().addAll("action-btn", "action-btn-danger");
                box.setAlignment(Pos.CENTER_LEFT);

                editBtn.setOnAction(e -> {
                    FacilityKRT facility = getFacility();
                    if (facility != null) showFacilityDialog(facility);
                });
                toggleBtn.setOnAction(e -> toggleFacility(getFacility()));
                deleteBtn.setOnAction(e -> deleteFacility(getFacility()));
            }

            private FacilityKRT getFacility() {
                int index = getIndex();
                if (index < 0 || index >= getTableView().getItems().size()) return null;
                return getTableView().getItems().get(index);
            }

            private void updateToggleButton(FacilityKRT facility) {
                toggleBtn.setText(facility.isActive() ? "Disable" : "Enable");
                toggleBtn.getStyleClass().removeAll("action-btn-enable", "action-btn-disable");
                toggleBtn.getStyleClass().add(facility.isActive() ? "action-btn-disable" : "action-btn-enable");
            }

            @Override protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty);
                FacilityKRT facility = empty ? null : getFacility();
                setGraphic(facility == null ? null : box);
                if (facility != null) updateToggleButton(facility);
            }
        });

        addBtn.setOnAction(e -> showFacilityDialog(null));
        refresh();
    }

    private void toggleFacility(FacilityKRT facility) {
        if (facility == null) return;
        if (!facility.isActive()) {
            if (facilityService.enableFacility(facility.getId())) refresh();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Disable \"" + facility.getName() + "\"? It will no longer appear in booking pages.",
                ButtonType.YES, ButtonType.NO);
        alert.showAndWait().filter(ButtonType.YES::equals).ifPresent(result -> {
            if (facilityService.disableFacility(facility.getId())) refresh();
        });
    }

    private void deleteFacility(FacilityKRT facility) {
        if (facility == null) return;
        if (facilityService.hasBookings(facility.getId())) {
            showError("This facility has booking records and cannot be permanently deleted. Disable it instead.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Permanently delete \"" + facility.getName() + "\"? This cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        alert.showAndWait().filter(ButtonType.YES::equals).ifPresent(result -> {
            if (facilityService.hardDelete(facility.getId())) refresh();
            else showError("Unable to delete the facility.");
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }

    private void refresh() {
        facilityTable.getItems().setAll(facilityService.getAll());
    }

    private void showFacilityDialog(FacilityKRT existing) {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle(existing == null ? "Add Facility" : "Edit Facility");

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(18));

        TextField name = new TextField(existing != null ? existing.getName() : "");
        ComboBox<FacilityKRT.Type> type = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(FacilityKRT.Type.values()));
        type.setValue(existing != null ? existing.getType() : FacilityKRT.Type.CLASSROOM);
        TextField location = new TextField(existing != null ? existing.getLocation() : "");
        TextField capacity = new TextField(existing != null ? String.valueOf(existing.getCapacity()) : "");
        TextField description = new TextField(existing != null ? existing.getDescription() : "");

        g.add(new Label("Name:"), 0, 0); g.add(name, 1, 0);
        g.add(new Label("Type:"), 0, 1); g.add(type, 1, 1);
        g.add(new Label("Location:"), 0, 2); g.add(location, 1, 2);
        g.add(new Label("Capacity:"), 0, 3); g.add(capacity, 1, 3);
        g.add(new Label("Description:"), 0, 4); g.add(description, 1, 4);

        Label msg = new Label();
        msg.setTextFill(Color.web("#f0847d"));

        Button save = new Button(existing == null ? "Add" : "Save");
        Button cancel = new Button("Cancel");
        HBox btnRow = new HBox(10, save, cancel);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        save.setOnAction(e -> {
            String n = name.getText().trim();
            String l = location.getText().trim();
            String c = capacity.getText().trim();
            if (n.isEmpty() || l.isEmpty() || c.isEmpty() || type.getValue() == null) {
                msg.setText("Name, location, capacity, and type are required.");
                return;
            }
            int cap;
            try {
                cap = Integer.parseInt(c);
                if (cap <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                msg.setText("Capacity must be a positive number.");
                return;
            }
            if (existing == null) {
                facilityService.addFacility(n, type.getValue(), l, cap, description.getText().trim());
            } else {
                facilityService.updateFacility(existing.getId(), n, type.getValue(), l, cap, description.getText().trim());
            }
            refresh();
            dlg.close();
        });
        cancel.setOnAction(e -> dlg.close());

        VBox root = new VBox(12, g, msg, btnRow);
        root.setPadding(new Insets(10));
        dlg.setScene(new Scene(root, 440, 300));
        dlg.showAndWait();
    }
}
