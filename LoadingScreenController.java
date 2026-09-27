package com.school.controller;

import com.school.util.LoadingSpinner;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class LoadingScreenController {

    @FXML private LoadingSpinner spinner;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        spinner.start();
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void stopSpinning() {
        spinner.stop();
    }
}
