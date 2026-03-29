package com.corepulse;

import com.corepulse.PulseEngine.HardwareSnapshot;
// Importing all the UI elements
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DashboardUI extends Application { // MAIN CLASS [Standard JAvaFX graphical application]

    private static final int MAX_DATA_POINTS = 60; // Active tick limit [on Graph]

    
    private XYChart.Series<Number, Number> cpuSeries; // Container 1

    private XYChart.Series<Number, Number> ramSeries; // Container 2

// Variables for holding UI Text Elements 

    private Label cpuLabel;

    private Label ramLabel;

    private Label uptimeLabel;

    private PulseEngine pulseEngine; // Reference for backend engine

    private int tickCount = 0; // [X-Axis : Time]

    
// UI Begins

    public void start(Stage stage) {

        pulseEngine = new PulseEngine(); // Backend Engine

        // Whenever pulseEngine emits a pulse, call onNewSnapshot()
        pulseEngine.setOnPulse(this::onNewSnapshot);

        NumberAxis cpuXAxis = createTimeAxis();
        NumberAxis cpuYAxis = new NumberAxis(0, 100, 10); // Hardcoded [0-100]
        cpuYAxis.setLabel("CPU (%)");
        cpuYAxis.setAutoRanging(false);

        LineChart<Number, Number> cpuChart = new LineChart<>(cpuXAxis, cpuYAxis);
        cpuChart.setTitle("CPU Usage");

// Turning off chart animations and symbols for a cleaner look

        cpuChart.setAnimated(false);
        cpuChart.setCreateSymbols(false);
        cpuChart.setLegendVisible(false);

        cpuSeries = new XYChart.Series<>();
        cpuChart.getData().add(cpuSeries);
        VBox.setVgrow(cpuChart, Priority.ALWAYS); // If window is resized, chart should stretch to fill the available vertical space evenly

        NumberAxis ramXAxis = createTimeAxis();
        NumberAxis ramYAxis = new NumberAxis();
        ramYAxis.setLabel("RAM (GB)");

        ramYAxis.setAutoRanging(true); //Ram varies system to system

        LineChart<Number, Number> ramChart = new LineChart<>(ramXAxis, ramYAxis);
        ramChart.setTitle("RAM Usage");
        ramChart.setAnimated(false);
        ramChart.setCreateSymbols(false);
        ramChart.setLegendVisible(false);

        ramSeries = new XYChart.Series<>();
        ramChart.getData().add(ramSeries);
        VBox.setVgrow(ramChart, Priority.ALWAYS);

// Initializing Labels

        cpuLabel    = styledLabel("CPU: --",     15, true);
        ramLabel    = styledLabel("RAM: --",     15, true);
        uptimeLabel = styledLabel("Uptime: --",  13, false);

// Horizontal arranging with 30-pixels spacesz

        HBox statsBar = new HBox(30, cpuLabel, ramLabel, uptimeLabel);
        statsBar.setPadding(new Insets(8, 16, 8, 16));
        statsBar.setStyle("-fx-background-color: #181825;");

// Vertical Arranging [MASTER CONTAINER for the window]

        VBox root = new VBox(0, statsBar, cpuChart, ramChart);
        root.setStyle("-fx-background-color: #1e1e2e;");

        Scene scene = new Scene(root, 960, 680);
        stage.setTitle("CorePulse — Hardware Monitor  [40% Demo]");
        stage.setScene(scene);
        stage.show();

// UI is ready -> Call Backend Engine

        pulseEngine.start();
    }
    
    public void stop() { // Shuts down pulseEngine
        if(pulseEngine != null) {
            pulseEngine.stop();
        }
    }

    private void onNewSnapshot(HardwareSnapshot snapshot) {
        tickCount++;

        cpuSeries.getData().add(new XYChart.Data<>(tickCount, snapshot.cpuUsage()));
        ramSeries.getData().add(new XYChart.Data<>(tickCount, snapshot.usedRamGb()));

        if (cpuSeries.getData().size() > MAX_DATA_POINTS) {
            cpuSeries.getData().remove(0);
        }
        if (ramSeries.getData().size() > MAX_DATA_POINTS) {
            ramSeries.getData().remove(0);
        }

        int lowerBound = Math.max(0, tickCount - MAX_DATA_POINTS);
        int upperBound = lowerBound + MAX_DATA_POINTS;

        NumberAxis cpuXAxis = (NumberAxis) cpuSeries.getChart().getXAxis();
        cpuXAxis.setLowerBound(lowerBound);
        cpuXAxis.setUpperBound(upperBound);

        NumberAxis ramXAxis = (NumberAxis) ramSeries.getChart().getXAxis();
        ramXAxis.setLowerBound(lowerBound);
        ramXAxis.setUpperBound(upperBound);

        cpuLabel.setText(String.format("CPU: %.1f%%", snapshot.cpuUsage()));
        ramLabel.setText(String.format(
                "RAM: %.2f / %.2f GB",
                snapshot.usedRamGb(),
                snapshot.totalRamGb()
        ));
        uptimeLabel.setText("Uptime: " + formatUptime(snapshot.uptimeSeconds()));
    }

    private NumberAxis createTimeAxis() {
        NumberAxis axis = new NumberAxis(0, MAX_DATA_POINTS, 5);
        axis.setLabel("Time (s)");
        axis.setAutoRanging(false);
        axis.setTickUnit(5);
        return axis;
    }

    private Label styledLabel(String text, int size, boolean bold) {
        Label label = new Label(text);
        String weight = bold ? "bold" : "normal";
        label.setStyle(String.format(
                "-fx-font-size: %dpx; -fx-font-weight: %s; -fx-text-fill: #cdd6f4;",
                size, weight
        ));
        return label;
    }

    private String formatUptime(long totalSeconds) {
        long days    = totalSeconds / 86_400;
        long hours   = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
    }
}
