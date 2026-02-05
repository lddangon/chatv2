package com.chatv2.server.gui.controller;

import com.chatv2.server.gui.ServerAdminApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Controller for the Log Viewer view.
 * Displays server logs in real-time with filtering capabilities.
 */
public class LogViewerController {
    private static final Logger log = LoggerFactory.getLogger(LogViewerController.class);

    private ServerAdminApp mainApp;
    private boolean autoScroll = true;
    private LogAppender customAppender;
    private final ConcurrentLinkedQueue<String> logBuffer = new ConcurrentLinkedQueue<>();
    private static final int MAX_LOG_ENTRIES = 1000;

    @FXML private TextArea logTextArea;
    @FXML private ComboBox<Level> levelFilterComboBox;
    @FXML private TextField searchField;
    @FXML private Button clearButton;
    @FXML private Button refreshButton;
    @FXML private Button exportButton;
    @FXML private CheckBox autoScrollCheckBox;

    /**
     * Sets the main application instance.
     *
     * @param mainApp ServerAdminApp instance
     */
    public void setMainApp(ServerAdminApp mainApp) {
        this.mainApp = mainApp;
    }

    /**
     * Initializes the controller.
     * Sets up custom Log4j2 appender and initializes UI components.
     */
    @FXML
    private void initialize() {
        log.debug("Initializing LogViewerController");

        // Initialize level filter combo box
        levelFilterComboBox.getItems().addAll(
            Level.ALL, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR
        );
        levelFilterComboBox.setValue(Level.ALL);

        // Initialize auto-scroll checkbox
        autoScrollCheckBox.setSelected(true);

        // Set log text area to be non-editable and monospaced
        logTextArea.setEditable(false);
        logTextArea.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 12px;");

        // Setup custom Log4j2 appender
        setupCustomAppender();

        // Add listeners
        setupListeners();

        log.info("LogViewerController initialized");
    }

    /**
     * Sets up custom Log4j2 appender to capture logs.
     */
    private void setupCustomAppender() {
        try {
            PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")
                .build();

            customAppender = new LogAppender("GuiLogAppender", layout, null, true);

            // Add appender to root logger
            org.apache.logging.log4j.core.LoggerContext context =
                (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
            org.apache.logging.log4j.core.Logger rootLogger = context.getRootLogger();
            rootLogger.addAppender(customAppender);

            log.debug("Custom Log4j2 appender registered");
        } catch (Exception e) {
            log.error("Failed to setup custom log appender", e);
        }
    }

    /**
     * Sets up listeners for UI components.
     */
    private void setupListeners() {
        // Search field listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterLogs());

        // Auto-scroll checkbox listener
        autoScrollCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            autoScroll = newValue;
            if (autoScroll) {
                logTextArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }

    /**
     * Handles Clear button click.
     */
    @FXML
    private void handleClear() {
        log.debug("Clear button clicked");
        logBuffer.clear();
        Platform.runLater(() -> logTextArea.clear());
    }

    /**
     * Handles Refresh button click.
     */
    @FXML
    private void handleRefresh() {
        log.debug("Refresh button clicked");
        filterLogs();
    }

    /**
     * Handles Export button click.
     */
    @FXML
    private void handleExport() {
        log.debug("Export button clicked");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Logs");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        fileChooser.setInitialFileName("chat-server-logs-" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")) + ".txt");

        File file = fileChooser.showSaveDialog(mainApp.getPrimaryStage());

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(logTextArea.getText());
                mainApp.showInfoAlert("Export Successful", "Logs Exported",
                    "Logs have been saved to: " + file.getAbsolutePath());
                log.info("Logs exported to: {}", file.getAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to export logs", e);
                mainApp.showErrorAlert("Export Failed", "Failed to export logs", e.getMessage());
            }
        }
    }

    /**
     * Filters logs based on selected level and search text.
     */
    private void filterLogs() {
        Level selectedLevel = levelFilterComboBox.getValue();
        String searchText = searchField.getText().toLowerCase().trim();

        StringBuilder filteredLogs = new StringBuilder();
        for (String logEntry : logBuffer) {
            boolean levelMatch = selectedLevel == Level.ALL ||
                logEntry.contains(selectedLevel.toString());

            boolean searchMatch = searchText.isEmpty() ||
                logEntry.toLowerCase().contains(searchText);

            if (levelMatch && searchMatch) {
                filteredLogs.append(logEntry).append("\n");
            }
        }

        String finalLogs = filteredLogs.toString();
        Platform.runLater(() -> {
            logTextArea.setText(finalLogs);
            if (autoScroll) {
                logTextArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }

    /**
     * Cleans up resources when controller is destroyed.
     */
    public void cleanup() {
        log.debug("Cleaning up LogViewerController");
        if (customAppender != null) {
            customAppender.stop();
            try {
                org.apache.logging.log4j.core.LoggerContext context =
                    (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
                org.apache.logging.log4j.core.Logger rootLogger = context.getRootLogger();
                rootLogger.removeAppender(customAppender);
            } catch (Exception e) {
                log.error("Error removing log appender", e);
            }
        }
    }

    /**
     * Custom Log4j2 Appender that captures log events for GUI display.
     */
    private class LogAppender extends AbstractAppender {
        protected LogAppender(String name, PatternLayout layout, Property[] properties, boolean ignoreExceptions) {
            super(name, null, layout, ignoreExceptions);
            start();
        }

        @Override
        public void append(LogEvent event) {
            if (!isStarted()) {
                return;
            }

            Level selectedLevel = levelFilterComboBox.getValue();
            if (selectedLevel != null && selectedLevel != Level.ALL &&
                event.getLevel().isMoreSpecificThan(selectedLevel)) {
                return;
            }

            String formattedLog = event.getMessage().getFormattedMessage();
            String timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeMillis()),
                ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            String level = event.getLevel().toString();
            String loggerName = event.getLoggerName();
            String thread = event.getThreadName();

            // Color code based on level
            String logLine = String.format("%s [%-15s] %-5s %-30s - %s",
                timestamp, thread, level, loggerName, formattedLog);

            // Add color ANSI codes (for potential future terminal export)
            String colorCode = getColorForLevel(event.getLevel());
            String coloredLog = colorCode + logLine + "\033[0m";

            // Add to buffer (limited size)
            logBuffer.offer(coloredLog);
            while (logBuffer.size() > MAX_LOG_ENTRIES) {
                logBuffer.poll();
            }

            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                String searchText = searchField.getText().toLowerCase().trim();
                boolean searchMatch = searchText.isEmpty() ||
                    logLine.toLowerCase().contains(searchText);

                if (searchMatch) {
                    logTextArea.appendText(logLine + "\n");

                    // Limit text area size
                    if (logTextArea.getText().split("\n").length > MAX_LOG_ENTRIES) {
                        String[] lines = logTextArea.getText().split("\n", -1);
                        int linesToRemove = lines.length - MAX_LOG_ENTRIES;
                        String newText = String.join("\n", java.util.Arrays.copyOfRange(lines, linesToRemove, lines.length));
                        logTextArea.setText(newText);
                    }

                    if (autoScroll) {
                        logTextArea.setScrollTop(Double.MAX_VALUE);
                    }
                }
            });
        }

        /**
         * Gets ANSI color code for log level.
         *
         * @param level the log level
         * @return ANSI color code
         */
        private String getColorForLevel(Level level) {
            return switch (level.getStandardLevel()) {
                case DEBUG -> "\033[90m"; // Gray
                case INFO -> "\033[94m";  // Blue
                case WARN -> "\033[93m";  // Yellow
                case ERROR -> "\033[91m"; // Red
                case FATAL -> "\033[95m"; // Magenta
                default -> "\033[0m";     // Default
            };
        }
    }
}
