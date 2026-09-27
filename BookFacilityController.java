package com.school.controller;

import com.school.model.Booking;
import com.school.model.FacilityKRT;
import com.school.model.User;
import com.school.service.BookServ;
import com.school.service.FacilservJRD;
import com.school.util.SessMgrKRT;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class BookFacilityController {

    private static final int TOTAL_STEPS = 6;

    private final FacilservJRD facilityService = new FacilservJRD();
    private final BookServ bookingService = new BookServ();

    @FXML private VBox step1Pane, step2Pane, step3Pane, step4Pane, step5Pane, step6Pane;
    @FXML private Button dateToggleBtn;
    @FXML private VBox datePickerBox;
    @FXML private AnchorPane bookingDatePickerTemplate;
    private DatePicker datePicker;
    @FXML private TextField searchField;
    @FXML private FlowPane roomCardFlow;
    @FXML private FlowPane startSlotFlow;
    @FXML private FlowPane endSlotFlow;
    @FXML private Label stepCountLabel;
    @FXML private Label reviewRoom, reviewDate, reviewStart, reviewEnd, confirmMsg;
    @FXML private Button backBtn, nextBtn, cancelBtn;

    private int step = 1;
    private LocalDate selectedDate;
    private FacilityKRT selectedFacility;
    private String selectedStart;
    private String selectedEnd;

    /** In-memory copy of the facility list — typing filters this, never the database. */
    private List<FacilityKRT> allRooms = List.of();
    /** Waits until the user pauses typing before rebuilding cards. */
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(200));
    /** Bumps on every slot fetch; late background results with an old id are dropped. */
    private int slotRequestId = 0;
    /** Last successfully requested slot contexts — avoids refetching on every click. */
    private String lastStartKey;
    private String lastEndKey;

    private record SlotStat(String start, boolean available) {}

    @FXML
    public void initialize() {
        datePicker = (DatePicker) bookingDatePickerTemplate.lookup("#datePicker");
        selectedDate = LocalDate.now().plusDays(1);
        datePicker.setValue(selectedDate);
        updateDateToggleText();

        dateToggleBtn.setOnAction(e -> {
            boolean open = !datePickerBox.isVisible();
            datePickerBox.setVisible(open);
            datePickerBox.setManaged(open);
        });

        datePicker.valueProperty().addListener((o, old, val) -> {
            selectedDate = val;
            updateDateToggleText();
            clearTimes();
            render();
        });

        searchDebounce.setOnFinished(e -> buildRoomCards());
        searchField.textProperty().addListener((o, old, val) -> {
            if (step == 3) searchDebounce.playFromStart();
        });

        backBtn.setOnAction(e -> { if (step > 1) { step--; render(); } });
        nextBtn.setOnAction(e -> onNext());
        cancelBtn.setOnAction(e -> onCancel());

        refreshRooms();
        render();
    }

    /** Fetch facilities in the background; keystrokes only filter the cached copy. */
    private void refreshRooms() {
        Task<List<FacilityKRT>> load = new Task<>() {
            @Override
            protected List<FacilityKRT> call() {
                return facilityService.getActive();
            }
        };
        load.setOnSucceeded(e -> {
            allRooms = load.getValue() == null ? List.of() : load.getValue();
            buildRoomCards();
        });
        load.setOnFailed(e -> buildRoomCards());
        new Thread(load).start();
    }

    // ──────────────────── STEP 2 & 3: search + room selection ────────────────────

    private void buildRoomCards() {
        roomCardFlow.getChildren().clear();
        // Case-insensitive: both the query and the haystack are lowercased,
        // so stored names can keep their original casing.
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ENGLISH);
        for (FacilityKRT room : allRooms) {
            if (!q.isEmpty()) {
                String hay = (room.getName() + " " + room.getLocation() + " " + room.getType())
                        .toLowerCase(Locale.ENGLISH);
                if (!hay.contains(q)) continue;
            }
            roomCardFlow.getChildren().add(buildRoomCard(room));
        }
    }

    private Button buildRoomCard(FacilityKRT room) {
        Button card = new Button(room.getName() + "\n" + room.getType() + "  ·  " + room.getLocation()
                + "\nCapacity " + room.getCapacity());
        card.getStyleClass().add("room-card");
        card.setMaxWidth(10000);
        // Keep the highlight when cards rebuild (e.g. while typing).
        if (selectedFacility != null && selectedFacility.getId().equals(room.getId())) {
            card.getStyleClass().add("room-card-selected");
        }
        card.setOnAction(e -> {
            selectedFacility = room;
            roomCardFlow.getChildren().forEach(n -> n.getStyleClass().remove("room-card-selected"));
            card.getStyleClass().add("room-card-selected");
            clearTimes();
            render();
        });
        return card;
    }

    // ──────────────────── STEPS 4 & 5: time slots ────────────────────

    private void buildStartSlots() {
        startSlotFlow.getChildren().clear();
        if (selectedFacility == null || selectedDate == null) return;
        Button loading = new Button("Checking…");
        loading.getStyleClass().add("time-slot");
        loading.setDisable(true);
        startSlotFlow.getChildren().add(loading);

        final int request = ++slotRequestId;
        final FacilityKRT facility = selectedFacility;
        final LocalDate date = selectedDate;
        final String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        Task<List<SlotStat>> load = new Task<>() {
            @Override
            protected List<SlotStat> call() {
                List<SlotStat> out = new ArrayList<>();
                for (int h = 7; h < 20 && !isCancelled(); h++) {
                    String s = String.format("%02d:00", h);
                    String e = String.format("%02d:00", h + 1);
                    out.add(new SlotStat(s, bookingService.isAvailable(facility.getId(), dateStr, s, e)));
                }
                return out;
            }
        };
        load.setOnSucceeded(e -> {
            if (request != slotRequestId || step != 4
                    || selectedFacility != facility || !date.equals(selectedDate)) return; // stale
            startSlotFlow.getChildren().clear();
            for (SlotStat slot : load.getValue()) {
                Button b = new Button(to12Hour(slot.start()));
                b.getStyleClass().add("time-slot");
                b.setUserData(slot.start());
                b.setDisable(!slot.available());
                if (slot.start().equals(selectedStart)) b.getStyleClass().add("time-slot-selected");
                b.setOnAction(ev -> {
                    selectedStart = slot.start();
                    selectedEnd = null;
                    render();
                });
                startSlotFlow.getChildren().add(b);
            }
        });
        load.setOnFailed(e -> {
            if (request != slotRequestId || step != 4) return;
            startSlotFlow.getChildren().clear();
            Button retry = new Button("Couldn't load slots — click to retry");
            retry.getStyleClass().add("time-slot");
            retry.setOnAction(ev -> buildStartSlots());
            startSlotFlow.getChildren().add(retry);
        });
        Thread t = new Thread(load, "slot-loader");
        t.setDaemon(true);
        t.start();
    }

    private void buildEndSlots() {
        endSlotFlow.getChildren().clear();
        if (selectedFacility == null || selectedDate == null || selectedStart == null) return;
        Button loading = new Button("Checking…");
        loading.getStyleClass().add("time-slot");
        loading.setDisable(true);
        endSlotFlow.getChildren().add(loading);

        final int request = ++slotRequestId;
        final FacilityKRT facility = selectedFacility;
        final LocalDate date = selectedDate;
        final String start = selectedStart;
        final String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        final int startHour = Integer.parseInt(start.substring(0, 2));
        Task<List<SlotStat>> load = new Task<>() {
            @Override
            protected List<SlotStat> call() {
                List<SlotStat> out = new ArrayList<>();
                for (int h = startHour + 1; h <= 20 && !isCancelled(); h++) {
                    String e = String.format("%02d:00", h);
                    out.add(new SlotStat(e, bookingService.isAvailable(facility.getId(), dateStr, start, e)));
                }
                return out;
            }
        };
        load.setOnSucceeded(e -> {
            if (request != slotRequestId || step != 5
                    || selectedFacility != facility || !date.equals(selectedDate)
                    || !start.equals(selectedStart)) return; // stale
            endSlotFlow.getChildren().clear();
            for (SlotStat slot : load.getValue()) {
                Button b = new Button(to12Hour(slot.start()));
                b.getStyleClass().add("time-slot");
                b.setUserData(slot.start());
                b.setDisable(!slot.available());
                if (slot.start().equals(selectedEnd)) b.getStyleClass().add("time-slot-selected");
                b.setOnAction(ev -> {
                    selectedEnd = slot.start();
                    render();
                });
                endSlotFlow.getChildren().add(b);
            }
        });
        load.setOnFailed(e -> {
            if (request != slotRequestId || step != 5) return;
            endSlotFlow.getChildren().clear();
            Button retry = new Button("Couldn't load slots — click to retry");
            retry.getStyleClass().add("time-slot");
            retry.setOnAction(ev -> buildEndSlots());
            endSlotFlow.getChildren().add(retry);
        });
        Thread t = new Thread(load, "slot-loader");
        t.setDaemon(true);
        t.start();
    }

    /** Re-highlight selection without refetching availability. */
    private void markSelected(FlowPane flow, String hhmm) {
        for (var n : flow.getChildren()) {
            if (n instanceof Button b && b.getUserData() instanceof String h) {
                b.getStyleClass().remove("time-slot-selected");
                if (h.equals(hhmm)) b.getStyleClass().add("time-slot-selected");
            }
        }
    }

    private String to12Hour(String hhmm) {
        int h = Integer.parseInt(hhmm.substring(0, 2));
        int hour = h == 0 ? 12 : h > 12 ? h - 12 : h;
        String suffix = h < 12 ? "AM" : "PM";
        return String.format("%02d:00 %s", hour, suffix);
    }

    private String formatDate(LocalDate d) {
        return d.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH));
    }

    private void updateDateToggleText() {
        dateToggleBtn.setText(selectedDate == null ? "Date: Not selected" : "Date: " + formatDate(selectedDate));
    }

    private void clearTimes() {
        selectedStart = null;
        selectedEnd = null;
    }

    // ──────────────────── NAV / CONFIRM / CANCEL ────────────────────

    private void onNext() {
        if (step < TOTAL_STEPS) {
            step++;
            if (step == 3) refreshRooms();
            render();
            return;
        }
        // step === TOTAL_STEPS: Confirm & Save
        if (selectedFacility == null || selectedDate == null || selectedStart == null || selectedEnd == null) return;

        User user = SessMgrKRT.getCurrentUser();
        String dateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        Optional<Booking> result = bookingService.createBooking(
                user.getId(), user.getName(), selectedFacility.getId(), selectedFacility.getName(),
                dateStr, selectedStart, selectedEnd, "General Use");

        if (result.isPresent()) {
            confirmMsg.setText("Request submitted for " + selectedFacility.getName() + " on " + formatDate(selectedDate)
                    + " " + to12Hour(selectedStart) + " – " + to12Hour(selectedEnd) + " (pending approval)");
            confirmMsg.getStyleClass().setAll("confirm-msg", "confirm-msg-ok");
            resetWizard();
        } else {
            confirmMsg.setText("That slot was just taken — pick a different time.");
            confirmMsg.getStyleClass().setAll("confirm-msg", "confirm-msg-error");
        }
    }

    private void onCancel() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel this booking? Your selections will be cleared.", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(r -> { if (r == ButtonType.YES) resetWizard(); });
    }

    private void resetWizard() {
        step = 1;
        selectedFacility = null;
        clearTimes();
        lastStartKey = null;
        lastEndKey = null;
        selectedDate = LocalDate.now().plusDays(1);
        datePicker.setValue(selectedDate);
        searchField.clear();
        datePickerBox.setVisible(false);
        datePickerBox.setManaged(false);
        buildRoomCards();
        render();
    }

    // ──────────────────── RENDER ────────────────────

    private void render() {
        step1Pane.setVisible(step == 1); step1Pane.setManaged(step == 1);
        step2Pane.setVisible(step == 2); step2Pane.setManaged(step == 2);
        step3Pane.setVisible(step == 3); step3Pane.setManaged(step == 3);
        step4Pane.setVisible(step == 4); step4Pane.setManaged(step == 4);
        step5Pane.setVisible(step == 5); step5Pane.setManaged(step == 5);
        step6Pane.setVisible(step == 6); step6Pane.setManaged(step == 6);

        stepCountLabel.setText(step + " of " + TOTAL_STEPS);
        backBtn.setVisible(step > 1);
        nextBtn.setText(step == TOTAL_STEPS ? "Confirm & Save" : "Next");

        boolean ready = switch (step) {
            case 1 -> selectedDate != null;
            case 2 -> true;
            case 3 -> selectedFacility != null;
            case 4 -> selectedStart != null;
            case 5 -> selectedEnd != null;
            default -> true;
        };
        nextBtn.setDisable(!ready);

        updateDateToggleText();
        if (step == 4) {
            // Refetch only when room/date changed — a plain selection click
            // just re-highlights instead of hitting the database 13 more times.
            String key = (selectedFacility == null || selectedDate == null) ? null
                    : selectedFacility.getId() + "|" + selectedDate;
            if (key == null || !key.equals(lastStartKey)) {
                lastStartKey = key;
                buildStartSlots();
            } else {
                markSelected(startSlotFlow, selectedStart);
            }
        }
        if (step == 5) {
            String key = (selectedFacility == null || selectedDate == null || selectedStart == null) ? null
                    : selectedFacility.getId() + "|" + selectedDate + "|" + selectedStart;
            if (key == null || !key.equals(lastEndKey)) {
                lastEndKey = key;
                buildEndSlots();
            } else {
                markSelected(endSlotFlow, selectedEnd);
            }
        }
        if (step == 6) {
            reviewRoom.setText(selectedFacility == null ? "—" : selectedFacility.getName() + " (" + selectedFacility.getType() + ")");
            reviewDate.setText(selectedDate == null ? "—" : formatDate(selectedDate));
            reviewStart.setText(selectedStart == null ? "—" : to12Hour(selectedStart));
            reviewEnd.setText(selectedEnd == null ? "—" : to12Hour(selectedEnd));
        }
    }
}