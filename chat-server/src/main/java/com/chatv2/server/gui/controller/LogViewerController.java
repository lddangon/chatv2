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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    
    // Flag to track if the custom appender was successfully configured
    private volatile boolean appenderConfigured = false;
    // Executor service for async appender initialization
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "LogViewer-Appender-Initializer");
        thread.setDaemon(true);
        return thread;
    });
    // Retry configuration for appender initialization
    private static final int APPENDER_INIT_RETRY_COUNT = 3;
    private static final long APPENDER_INIT_DELAY_MS = 500;
    private static final long APPENDER_INIT_RETRY_DELAY_MS = 1000;

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
     * Sets up UI components and schedules async appender initialization.
     * 
     * Note: Appender initialization is deferred to avoid potential issues with
     * Log4j2 context initialization during JavaFX startup. This is done
     * asynchronously after UI is fully initialized.
     */
    @FXML
    private void initialize() {
        log.debug("Initializing LogViewerController");

        try {
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

            // Add listeners for UI components
            setupListeners();

            // Load historical logs from file (if exists)
            // This shows logs that were generated BEFORE LogViewer was opened
            loadLogsFromFile();

            // Schedule async appender initialization for NEW logs
            initializeAppenderAsync();

            log.info("LogViewerController initialized successfully");
        } catch (Exception e) {
            log.error("Error during LogViewerController initialization", e);
            // Still try to load logs and initialize appender on error
            loadLogsFromFile();
            initializeAppenderAsync();
        }
    }

    /**
     * Asynchronously initializes the custom Log4j2 appender with retry logic.
     * 
     * This method is called after UI initialization to ensure that:
     * 1. Log4j2 context is fully initialized
     * 2. JavaFX Application Thread is not blocked
     * 3. UI components are ready before appender starts sending events
     * 
     * The initialization includes:
     * - Initial delay to allow Log4j2 context to stabilize
     * - Multiple retry attempts on failure
     * - Detailed error logging for debugging
     * - Fallback to reading logs from file if appender setup fails
     */
    private void initializeAppenderAsync() {
        executorService.submit(() -> {
            log.debug("Starting async appender initialization with {} retries", APPENDER_INIT_RETRY_COUNT);
            
            // Wait for Log4j2 context to be fully initialized
            try {
                Thread.sleep(APPENDER_INIT_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Appender initialization delay was interrupted", e);
                return;
            }

            // Retry mechanism for appender initialization
            int attempt = 0;
            boolean success = false;
            Exception lastException = null;

            while (attempt < APPENDER_INIT_RETRY_COUNT && !success) {
                attempt++;
                try {
                    log.debug("Appender initialization attempt {}/{}", attempt, APPENDER_INIT_RETRY_COUNT);
                    success = setupCustomAppender();
                    
                    if (success) {
                        log.info("Custom Log4j2 appender initialized successfully on attempt {}", attempt);
                        appenderConfigured = true;
                        break;
                    }
                } catch (Exception e) {
                    lastException = e;
                    log.warn("Appender initialization attempt {}/{} failed: {}", 
                        attempt, APPENDER_INIT_RETRY_COUNT, e.getMessage());
                    
                    // Wait before retrying (except on last attempt)
                    if (attempt < APPENDER_INIT_RETRY_COUNT) {
                        try {
                            Thread.sleep(APPENDER_INIT_RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("Retry delay was interrupted", ie);
                            break;
                        }
                    }
                }
            }

            // Handle final result
            if (!success) {
                log.error("Failed to initialize custom Log4j2 appender after {} attempts", 
                    APPENDER_INIT_RETRY_COUNT, lastException);
                appenderConfigured = false;

                // Show warning to user on JavaFX Application Thread
                Platform.runLater(() -> {
                    try {
                        mainApp.showWarningAlert(
                            "Log Appender Not Configured",
                            "Live Log Capture Unavailable",
                            "The custom log appender could not be initialized. " +
                            "Historical logs from file are displayed, but new logs will not appear in real-time."
                        );
                        // Historical logs were already loaded in initialize(), so no need to reload here
                    } catch (Exception e) {
                        log.error("Error showing warning alert", e);
                    }
                });
            }
        });
    }

    /**
     * Sets up custom Log4j2 appender to capture logs in real-time.
     * 
     * This method:
     * - Checks if Log4j2 LoggerContext is properly initialized
     * - Creates and configures a custom appender
     * - Registers the appender with the root logger
     * - Returns true if successful, false otherwise
     * 
     * @return true if appender was successfully configured, false otherwise
     */
    private boolean setupCustomAppender() {
        try {
            // Check if Log4j2 context is properly initialized
            org.apache.logging.log4j.core.LoggerContext context;
            try {
                context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
                
                if (context == null) {
                    log.error("Log4j2 LoggerContext is null - cannot initialize appender");
                    return false;
                }
                
                // Verify context is started and accessible
                if (context.getState() != org.apache.logging.log4j.core.LifeCycle.State.STARTED) {
                    log.warn("Log4j2 LoggerContext is not started - attempting to use it anyway");
                }
            } catch (Exception e) {
                log.error("Error accessing Log4j2 LoggerContext", e);
                return false;
            }

            // Create pattern layout for log formatting
            PatternLayout layout;
            try {
                layout = PatternLayout.newBuilder()
                    .withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")
                    .build();
            } catch (Exception e) {
                log.error("Failed to create PatternLayout for appender", e);
                return false;
            }

            // Create custom appender instance
            try {
                customAppender = new LogAppender("GuiLogAppender", layout, null, true);
            } catch (Exception e) {
                log.error("Failed to instantiate LogAppender", e);
                return false;
            }

            // Add appender to root logger
            try {
                org.apache.logging.log4j.core.Logger rootLogger = context.getRootLogger();
                
                if (rootLogger == null) {
                    log.error("Root logger is null - cannot add appender");
                    return false;
                }
                
                rootLogger.addAppender(customAppender);
                log.debug("Custom Log4j2 appender registered to root logger");
            } catch (Exception e) {
                log.error("Failed to add appender to root logger", e);
                if (customAppender != null) {
                    customAppender.stop();
                    customAppender = null;
                }
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Unexpected error during appender setup", e);
            return false;
        }
    }

    /**
     * Loads existing logs from log file(s) as a fallback when appender is not configured.
     * 
     * This method searches for common log file locations and reads the most recent
     * log entries to populate the log viewer. It's called when the appender
     * initialization fails but we still want to show logs to the user.
     */
    private void loadLogsFromFile() {
        log.debug("Attempting to load logs from file as fallback");
        
        try {
            // Common log file locations to search
            // Based on log4j2.xml configuration:
            // - fileName="logs/chat.log"
            // - filePattern="logs/chat-%d{yyyy-MM-dd}-%i.log"
            String[] possibleLogPaths = {
                "logs/chat.log",
                "logs/application.log",
                "logs/server.log",
                "logs/chat-server.log",
                System.getProperty("user.dir") + "/logs/chat.log",
                System.getProperty("user.dir") + "/logs/application.log",
                System.getProperty("user.dir") + "/logs/server.log",
                System.getProperty("user.dir") + "/logs/chat-server.log"
            };

            boolean loaded = false;
            
            // First try to find and load the main log file
            for (String logPath : possibleLogPaths) {
                Path path = Paths.get(logPath);
                if (Files.exists(path) && Files.isReadable(path)) {
                    log.info("Found readable log file: {}", logPath);
                    if (readLogFile(path)) {
                        loaded = true;
                        break;
                    }
                }
            }
            
            // If main log file not found, try rolling files (with date pattern)
            if (!loaded) {
                log.debug("Main log file not found, searching for rolling files");
                try {
                    java.nio.file.DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get("logs"));
                    for (Path file : dirStream) {
                        String fileName = file.getFileName().toString();
                        // Look for rolling files: chat-YYYY-MM-DD-N.log
                        if (fileName.startsWith("chat-20") && fileName.endsWith(".log")) {
                            log.info("Found rolling log file: {}", file);
                            if (readLogFile(file)) {
                                loaded = true;
                                break;
                            }
                        }
                    }
                    dirStream.close();
                } catch (IOException e) {
                    log.debug("No logs directory found or error reading logs directory", e);
                }
            }

            if (!loaded) {
                log.warn("No readable log files found in expected locations");
                Platform.runLater(() -> {
                    logTextArea.appendText("Note: No historical log files found.\n");
                    logTextArea.appendText("Log viewer will display new logs as they are generated.\n");
                    logTextArea.appendText("Expected locations: logs/chat.log, logs/chat-YYYY-MM-DD-N.log\n");
                });
            }
        } catch (Exception e) {
            log.error("Error loading logs from file", e);
        }
    }

    /**
     * Reads a log file and populates the log buffer with recent entries.
     * 
     * @param logFilePath the path to the log file to read
     * @return true if file was read successfully, false otherwise
     */
    private boolean readLogFile(Path logFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath.toFile()))) {
            StringBuilder content = new StringBuilder();
            String line;
            int lineCount = 0;
            
            // Read the file
            while ((line = reader.readLine()) != null && lineCount < MAX_LOG_ENTRIES) {
                content.append(line).append("\n");
                lineCount++;
            }

            final String logs = content.toString();
            final int finalLineCount = lineCount;
            
            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                logTextArea.appendText("Loaded " + finalLineCount + " log entries from file:\n");
                logTextArea.appendText(logs);
                logTextArea.appendText("\n--- End of file contents ---\n");
            });

            // Also populate buffer for filtering
            String[] lines = logs.split("\n");
            for (String logLine : lines) {
                logBuffer.offer(logLine);
            }

            log.info("Successfully loaded {} log entries from file", lineCount);
            return true;
        } catch (IOException e) {
            log.error("Failed to read log file: {}", logFilePath, e);
            return false;
        }
    }

    /**
     * Sets up listeners for UI components.
     * 
     * This method configures event listeners for interactive UI elements:
     * - Search field: Triggers log filtering when text changes
     * - Auto-scroll checkbox: Toggles automatic scrolling to new logs
     * - Level filter combo box: Filters logs by severity level
     * 
     * All listeners are non-blocking and execute on the JavaFX Application Thread.
     */
    private void setupListeners() {
        // Search field listener - triggers log filtering when text changes
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterLogs());

        // Level filter combo box listener - triggers log filtering when selection changes
        levelFilterComboBox.setOnAction(event -> filterLogs());

        // Auto-scroll checkbox listener - toggles automatic scrolling to new logs
        autoScrollCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            autoScroll = newValue;
            if (autoScroll) {
                logTextArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }

    /**
     * Handles Clear button click.
     * Clears the log buffer and text area.
     */
    @FXML
    private void handleClear() {
        log.debug("Clear button clicked");
        logBuffer.clear();
        Platform.runLater(() -> logTextArea.clear());
    }

    /**
     * Handles Refresh button click.
     * Re-applies the current filters to the log buffer.
     */
    @FXML
    private void handleRefresh() {
        log.debug("Refresh button clicked");
        filterLogs();
    }

    /**
     * Handles Export button click.
     * Exports the currently displayed logs to a text file.
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
     * Works independently of appender configuration - filters from log buffer.
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
     * 
     * This method ensures proper resource cleanup to prevent memory leaks:
     * - Stops and removes the custom Log4j2 appender if configured
     * - Shuts down the executor service gracefully
     * - Clears the log buffer
     * - Handles any cleanup errors gracefully with detailed logging
     * 
     * This method should be called when:
     * - Switching away from the Log Viewer view
     * - The application is shutting down
     * - The controller is no longer needed
     */
    public void cleanup() {
        log.debug("Cleaning up LogViewerController");
        
        // Cleanup appender if it was configured
        if (customAppender != null) {
            try {
                log.debug("Stopping custom log appender");
                customAppender.stop();
                
                // Remove appender from root logger
                try {
                    org.apache.logging.log4j.core.LoggerContext context =
                        (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
                    
                    if (context != null) {
                        org.apache.logging.log4j.core.Logger rootLogger = context.getRootLogger();
                        
                        if (rootLogger != null) {
                            rootLogger.removeAppender(customAppender);
                            log.debug("Custom log appender removed from root logger");
                        }
                    }
                } catch (Exception e) {
                    log.error("Error removing log appender from logger", e);
                }
            } catch (Exception e) {
                log.error("Error during appender cleanup", e);
            }
        }

        // Shutdown executor service
        try {
            log.debug("Shutting down executor service");
            executorService.shutdown();
            
            // Wait for pending tasks to complete (max 2 seconds)
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                log.warn("Executor service did not terminate gracefully, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("Executor service shutdown was interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error shutting down executor service", e);
        }

        // Clear log buffer to free memory
        logBuffer.clear();

        log.info("LogViewerController cleanup completed");
    }

    /**
     * Custom Log4j2 Appender that captures log events for GUI display.
     * 
     * This appender:
     * - Receives log events from Log4j2
     * - Formats them with timestamp, level, logger name, etc.
     * - Filters based on selected log level using numeric comparison
     * - Updates the JavaFX UI via Platform.runLater()
     * - Maintains a bounded buffer to prevent memory issues
     * - Stores plain text (no ANSI codes) in logBuffer for UI
     */
    private class LogAppender extends AbstractAppender {
        protected LogAppender(String name, PatternLayout layout, Property[] properties, boolean ignoreExceptions) {
            super(name, null, layout, ignoreExceptions);
            start();
        }

        @Override
        public void append(LogEvent event) {
            // Skip if appender is not started
            if (!isStarted()) {
                return;
            }

            try {
                // Filter based on selected level using numeric comparison
                // Only skip logs that are WEAKER than the selected level
                Level selectedLevel = levelFilterComboBox.getValue();
                if (selectedLevel != null && selectedLevel != Level.ALL &&
                    event.getLevel().intLevel() < selectedLevel.intLevel()) {
                    return;  // Skip logs weaker than selected level
                }

                // Format log entry
                String formattedLog = event.getMessage().getFormattedMessage();
                String timestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(event.getTimeMillis()),
                    ZoneId.systemDefault()
                ).format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
                String level = event.getLevel().toString();
                String loggerName = event.getLoggerName();
                String thread = event.getThreadName();

                // Build log line (plain text, no ANSI codes)
                String logLine = String.format("%s [%-15s] %-5s %-30s - %s",
                    timestamp, thread, level, loggerName, formattedLog);

                // Store only plain text in buffer for UI (no ANSI codes)
                // ANSI codes are only used when exporting to file with colors
                logBuffer.offer(logLine);
                while (logBuffer.size() > MAX_LOG_ENTRIES) {
                    logBuffer.poll();
                }

                // Update UI on JavaFX Application Thread
                Platform.runLater(() -> {
                    try {
                        String searchText = searchField.getText().toLowerCase().trim();
                        boolean searchMatch = searchText.isEmpty() ||
                            logLine.toLowerCase().contains(searchText);

                        if (searchMatch) {
                            logTextArea.appendText(logLine + "\n");

                            // Limit text area size to prevent UI performance issues
                            String[] lines = logTextArea.getText().split("\n", -1);
                            if (lines.length > MAX_LOG_ENTRIES) {
                                int linesToRemove = lines.length - MAX_LOG_ENTRIES;
                                String newText = String.join("\n", 
                                    Arrays.copyOfRange(lines, linesToRemove, lines.length));
                                logTextArea.setText(newText);
                            }

                            // Auto-scroll if enabled
                            if (autoScroll) {
                                logTextArea.setScrollTop(Double.MAX_VALUE);
                            }
                        }
                    } catch (Exception e) {
                        // Catch UI update errors to prevent appender from failing
                        log.error("Error updating log viewer UI", e);
                    }
                });
            } catch (Exception e) {
                // Catch any processing errors to prevent appender from stopping
                log.error("Error processing log event", e);
            }
        }

        /**
         * Gets ANSI color code for log level.
         * These codes are used for terminal output when logs are exported with colors.
         * Note: This is NOT used for UI display - only for potential export functionality.
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
