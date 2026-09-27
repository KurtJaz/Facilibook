package com.school.util;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

/**
 * Spinning loader: the 8 spokes of the Lucide "loader" glyph, rotating
 * slowly. Use the no-arg constructor from FXML, or LoadingSpinner(radius)
 * from Java.
 */
public class LoadingSpinner extends StackPane {

    /** Lucide "loader" spokes in its native 24x24 box (x1, y1, x2, y2). */
    private static final double[][] SPOKES = {
        {12, 2, 12, 6},
        {16.2, 7.8, 19.1, 4.9},
        {18, 12, 22, 12},
        {16.2, 16.2, 19.1, 19.1},
        {12, 18, 12, 22},
        {4.9, 19.1, 7.8, 16.2},
        {2, 12, 6, 12},
        {4.9, 4.9, 7.8, 7.8},
    };

    private final Group spokesGroup = new Group();
    private final RotateTransition spin;

    public LoadingSpinner() {
        this(40);
    }

    public LoadingSpinner(double radius) {
        // Outer spoke tips sit 10 units from the icon centre — scale to radius.
        double s = radius / 10.0;
        double strokeWidth = Math.max(2, radius / 6.0);
        for (double[] sp : SPOKES) {
            Line line = new Line(
                    (sp[0] - 12) * s, (sp[1] - 12) * s,
                    (sp[2] - 12) * s, (sp[3] - 12) * s);
            line.setStroke(Color.web("#3b82f6"));
            line.setStrokeWidth(strokeWidth);
            line.setStrokeLineCap(StrokeLineCap.ROUND);
            spokesGroup.getChildren().add(line);
        }

        getChildren().add(spokesGroup);
        // Rasterize once and spin the cached bitmap: constant-velocity rotation
        // with no per-frame vector re-rasterization.
        spokesGroup.setCache(true);
        spokesGroup.setCacheHint(CacheHint.ROTATE);
        double size = radius * 2 + strokeWidth;
        setPrefSize(size, size);
        setMinSize(size, size);
        setMaxSize(size, size);

        spin = new RotateTransition(Duration.seconds(2.4), spokesGroup);
        spin.setByAngle(360);
        spin.setCycleCount(RotateTransition.INDEFINITE);
        spin.setInterpolator(Interpolator.LINEAR);
    }

    /** Sets the spoke color (hex, e.g. "#3b82f6"). */
    public void setColor(String hex) {
        Color color = Color.web(hex);
        spokesGroup.getChildren().forEach(n -> ((Line) n).setStroke(color));
    }

    public void start() { spin.play(); }
    public void stop() { spin.stop(); }
}
