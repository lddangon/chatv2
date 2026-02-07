package com.chatv2.server.gui.controller;

import com.chatv2.server.gui.ServerAdminApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;

/**
 * Controller for the Log Viewer view.
 * Displays server logs in real-time by monitoring the log file.
 */
public class LogViewerController {
    private static final Logger log = LoggerFactory.getLogger(LogViewerController.class);

    private ServerAdminApp mainApp;
    private boolean autoScroll = true;
    private final ConcurrentLinkedQueue<String> logBuffer = new ConcurrentLinkedQueue<>();
    private static final int MAX_LOG_ENTRIES = 1000;

    // Log file watching
    private ScheduledExecutorService logFileWatcher;
    private long lastLogFileSize = -1;
    private Path logFilePath;
    private volatile boolean isWatching = false;

    // Flag to prevent recursive logging when reading from file
    private volatile boolean isReadingFromFile = false;
    
    // Async log loading
    private ExecutorService logLoaderExecutor;
    private volatile boolean isLoadingLogs = false;

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
     * Sets up UI components and starts log file monitoring.
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

            // Find the log file
            findLogFile();

            // Initialize log loader executor
            logLoaderExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "LogViewer-LogLoader");
                thread.setDaemon(true);
                return thread;
            });

            // Load initial logs from file asynchronously
            loadLogsFromFileAsync();

            // Start watching log file for new entries
            startLogFileWatcher();

            log.info("LogViewerController initialized successfully");
        } catch (Exception e) {
            log.error("Error during LogViewerController initialization", e);
        }
    }

    /**
     * Finds the log file to monitor.
     * Searches common locations for log files.
     */
    private void findLogFile() {
        log.debug("Searching for log file");

        // Common log file locations
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

        // Try to find the main log file
        for (String logPath : possibleLogPaths) {
            Path path = Paths.get(logPath);
            if (Files.exists(path) && Files.isReadable(path)) {
                logFilePath = path;
                log.info("Found log file to monitor: {}", path.toAbsolutePath());
                return;
            }
        }

        // If main file not found, look for rolling files
        try (java.nio.file.DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get("logs"))) {
            for (Path file : dirStream) {
                String fileName = file.getFileName().toString();
                // Look for rolling files: chat-YYYY-MM-DD-N.log
                if (fileName.startsWith("chat-20") && fileName.endsWith(".log")) {
                    logFilePath = file;
                    log.info("Found rolling log file to monitor: {}", file.toAbsolutePath());
                    return;
                }
            }
        } catch (IOException e) {
            log.debug("No logs directory found or error reading logs directory", e);
        }

        if (logFilePath == null) {
            log.warn("No log file found to monitor");
            Platform.runLater(() -> {
                logTextArea.appendText("Warning: No log file found in expected locations.\n");
                logTextArea.appendText("Expected locations: logs/chat.log, logs/chat-YYYY-MM-DD-N.log\n");
            });
        }
    }

    /**
     * Starts the log file watcher service.
     * Creates a scheduled executor that checks the log file for changes periodically.
     */
    private void startLogFileWatcher() {
        if (logFilePath == null) {
            log.debug("Cannot start file watcher - no log file found");
            return;
        }

        log.debug("Starting log file watcher");

        // Initialize file size
        try {
            lastLogFileSize = Files.size(logFilePath);
            log.debug("Initial log file size: {} bytes", lastLogFileSize);
        } catch (IOException e) {
            log.error("Error getting initial log file size", e);
            return;
        }

        // Create daemon thread for file watching
        logFileWatcher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "LogViewer-FileWatcher");
            thread.setDaemon(true);
            return thread;
        });

        // Check log file every 500ms
        logFileWatcher.scheduleAtFixedRate(() -> {
            try {
                if (isWatching) {
                    checkLogForNewEntries();
                }
            } catch (Exception e) {
                log.error("Error in file watcher task", e);
            }
        }, 500, 500, TimeUnit.MILLISECONDS);

        isWatching = true;
        log.info("Log file watcher started, checking every 500ms");
    }

    /**
     * Checks the log file for new entries.
     * Compares current file size with last known size and reads new content if changed.
     * Prevents recursive logging that could cause infinite loop.
     */
    private void checkLogForNewEntries() {
        // CRITICAL: Prevent recursive calls - if we're already reading, return immediately
        // This protects against: LogViewer writes log → file grows → triggers reading again
        if (isReadingFromFile) {
            return;
        }

        try {
            if (logFilePath == null || !Files.exists(logFilePath)) {
                return;
            }

            long currentSize = Files.size(logFilePath);

            // Check if file grew (new entries added)
            if (currentSize > lastLogFileSize) {
                // CRITICAL: Prevent recursive logging!
                isReadingFromFile = true;

                try {
                    readNewLogEntries(logFilePath, lastLogFileSize, currentSize);
                } finally {
                    isReadingFromFile = false;
                }

                lastLogFileSize = currentSize;
            }
            // Handle file rotation (file was truncated and new one started)
            else if (currentSize < lastLogFileSize) {
                log.debug("Log file size decreased, possible rotation detected. Current: {}, Previous: {}",
                    currentSize, lastLogFileSize);

                // Reload entire file
                Platform.runLater(() -> {
                    logTextArea.appendText("\n--- Log file rotation detected, reloading ---\n");
                });
                lastLogFileSize = currentSize;
                loadLogsFromFileAsync();
            }

        } catch (IOException e) {
            log.error("Error checking log file for new entries", e);
        }
    }

    /**
     * Reads new log entries from the file starting from the specified position.
     *
     * @param logFile the log file path
     * @param startPosition the position to start reading from
     * @param endPosition the position to end reading at
     */
    private void readNewLogEntries(Path logFile, long startPosition, long endPosition) {
        try (RandomAccessFile raf = new RandomAccessFile(logFile.toFile(), "r")) {
            // Seek to last known position
            raf.seek(startPosition);

            // Read new content
            byte[] buffer = new byte[(int)(endPosition - startPosition)];
            int bytesRead = raf.read(buffer);

            if (bytesRead > 0) {
                String newContent = new String(buffer, 0, bytesRead);
                String[] lines = newContent.split("\n", -1);

                final Level selectedLevel = levelFilterComboBox.getValue();
                final String searchText = searchField.getText().toLowerCase().trim();
                int newEntriesCount = 0;

                for (String line : lines) {
                    if (line.isEmpty()) {
                        continue;
                    }

                    // Parse level from log line
                    Level lineLevel = parseLogLevel(line);

                    // Add to buffer
                    logBuffer.offer(line);
                    while (logBuffer.size() > MAX_LOG_ENTRIES) {
                        logBuffer.poll();
                    }

                    // Apply filters
                    boolean levelMatch = selectedLevel == Level.ALL ||
                        (lineLevel != null && lineLevel.intLevel() >= selectedLevel.intLevel());

                    boolean searchMatch = searchText.isEmpty() ||
                        line.toLowerCase().contains(searchText);

                    if (levelMatch && searchMatch) {
                        final String logLine = line;
                        Platform.runLater(() -> {
                            try {
                                // Add to buffer WITHOUT logging to prevent cycle
                                logTextArea.appendText(logLine + "\n");

                                // Limit text area size
                                String[] allLines = logTextArea.getText().split("\n", -1);
                                if (allLines.length > MAX_LOG_ENTRIES) {
                                    int linesToRemove = allLines.length - MAX_LOG_ENTRIES;
                                    String newText = String.join("\n",
                                        Arrays.copyOfRange(allLines, linesToRemove, allLines.length));
                                    logTextArea.setText(newText);
                                }

                                // Auto-scroll if enabled
                                if (autoScroll) {
                                    logTextArea.setScrollTop(Double.MAX_VALUE);
                                }
                            } catch (Exception e) {
                                // Log this error (it's not related to reading file)
                                log.error("Error updating log viewer UI", e);
                            }
                        });
                    }

                }
            }

        } catch (IOException e) {
            log.error("Error reading new log entries from file", e);
        }
    }

    /**
     * Parses log level from a log line.
     *
     * @param line the log line
     * @return the parsed Level, or null if not found
     */
    private Level parseLogLevel(String line) {
        // Log format: "HH:mm:ss.SSS [Thread] LEVEL LoggerName - Message"
        // Try to extract level between [Thread] and LoggerName
        int threadEnd = line.indexOf(']');
        if (threadEnd != -1 && threadEnd + 2 < line.length()) {
            String remaining = line.substring(threadEnd + 2);
            String[] parts = remaining.split(" ", 3);
            if (parts.length >= 2) {
                String levelStr = parts[0];
                try {
                    return Level.valueOf(levelStr);
                } catch (IllegalArgumentException e) {
                    // Not a valid level
                }
            }
        }
        return null;
    }

    /**
     * Loads existing logs from the log file.
     * Reads the most recent log entries up to MAX_LOG_ENTRIES.
     */
    private void loadLogsFromFile() {
        if (logFilePath == null) {
            log.debug("Cannot load logs - no log file found");
            return;
        }

        log.debug("Loading logs from file: {}", logFilePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath.toFile()))) {
            StringBuilder content = new StringBuilder();
            String line;
            int lineCount = 0;

            // Read last MAX_LOG_ENTRIES lines from file
            java.util.LinkedList<String> lines = new java.util.LinkedList<>();
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                if (lines.size() > MAX_LOG_ENTRIES) {
                    lines.removeFirst();
                }
                lineCount++;
            }

            // Build content from last lines
            for (String l : lines) {
                content.append(l).append("\n");
                logBuffer.offer(l);
            }

            final String logs = content.toString();
            final int finalLineCount = Math.min(lineCount, MAX_LOG_ENTRIES);

            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                if (finalLineCount > 0) {
                    logTextArea.appendText("Loaded " + finalLineCount + " recent log entries:\n");
                    logTextArea.appendText(logs);
                } else {
                    logTextArea.appendText("Log file is empty.\n");
                }
            });

            log.info("Successfully loaded {} log entries from file", finalLineCount);

        } catch (IOException e) {
            log.error("Failed to read log file: {}", logFilePath, e);
            Platform.runLater(() -> {
                logTextArea.appendText("Error reading log file. Check system logs for details.\n");
            });
        }
    }

    /**
     * Loads existing logs from the log file asynchronously.
     * Reads the most recent log entries up to MAX_LOG_ENTRIES.
     * This method does not block the JavaFX Application Thread.
     */
    private void loadLogsFromFileAsync() {
        if (logFilePath == null) {
            log.debug("Cannot load logs - no log file found");
            return;
        }

        if (!canLoadLogs()) {
            log.debug("Cannot load logs - either already loading or file not accessible");
            return;
        }

        log.debug("Loading logs from file asynchronously: {}", logFilePath);

        isLoadingLogs = true;
        logLoaderExecutor.submit(() -> {
            try {
                // Чтение файла (логика из loadLogsFromFile, но без log.info в конце)
                try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath.toFile()))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    int lineCount = 0;

                    // Read last MAX_LOG_ENTRIES lines from file
                    java.util.LinkedList<String> lines = new java.util.LinkedList<>();
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                        if (lines.size() > MAX_LOG_ENTRIES) {
                            lines.removeFirst();
                        }
                        lineCount++;
                    }

                    // Build content from last lines
                    for (String l : lines) {
                        content.append(l).append("\n");
                        logBuffer.offer(l);
                    }

                    final String logs = content.toString();
                    final int finalLineCount = Math.min(lineCount, MAX_LOG_ENTRIES);

                    // Update UI on JavaFX thread
                    Platform.runLater(() -> {
                        if (finalLineCount > 0) {
                            logTextArea.appendText("Loaded " + finalLineCount + " recent log entries:\n");
                            logTextArea.appendText(logs);
                        } else {
                            logTextArea.appendText("Log file is empty.\n");
                        }
                    });
                }
            } catch (IOException e) {
                log.error("Failed to read log file: {}", logFilePath, e);
                Platform.runLater(() -> {
                    logTextArea.appendText("Error reading log file. Check system logs for details.\n");
                });
            } finally {
                isLoadingLogs = false;
            }
        });
    }

    /**
     * Checks if logs can be loaded from file.
     * Ensures that no concurrent loading is happening and all resources are available.
     *
     * @return true if logs can be loaded, false otherwise
     */
    private boolean canLoadLogs() {
        return !isLoadingLogs && 
               logFilePath != null && 
               Files.exists(logFilePath) &&
               logLoaderExecutor != null && 
               !logLoaderExecutor.isShutdown();
    }

    /**
     * Sets up listeners for UI components.
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
     * Applies filters to the log buffer and updates the UI.
     */
    private void filterLogs() {
        Level selectedLevel = levelFilterComboBox.getValue();
        String searchText = searchField.getText().toLowerCase().trim();

        StringBuilder filteredLogs = new StringBuilder();
        for (String logEntry : logBuffer) {
            // Parse level from log entry
            Level entryLevel = parseLogLevel(logEntry);

            boolean levelMatch = selectedLevel == Level.ALL ||
                (entryLevel != null && entryLevel.intLevel() >= selectedLevel.intLevel());

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
     * Stops the file watcher and clears the log buffer.
     */
    public void cleanup() {
        log.debug("Cleaning up LogViewerController");

        // Stop file watching
        isWatching = false;

        // Shutdown file watcher
        if (logFileWatcher != null) {
            try {
                log.debug("Shutting down log file watcher");
                logFileWatcher.shutdown();

                // Wait for pending tasks to complete (max 1 second)
                if (!logFileWatcher.awaitTermination(1, TimeUnit.SECONDS)) {
                    log.warn("File watcher did not terminate gracefully, forcing shutdown");
                    logFileWatcher.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("File watcher shutdown was interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Error shutting down file watcher", e);
            }
        }

        // Stop log loading
        isLoadingLogs = false;
        if (logLoaderExecutor != null) {
            try {
                log.debug("Shutting down log loader executor");
                logLoaderExecutor.shutdown();
                if (!logLoaderExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.warn("Log loader executor did not terminate gracefully, forcing shutdown");
                    logLoaderExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("Log loader executor shutdown was interrupted", e);
                logLoaderExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Clear log buffer to free memory
        logBuffer.clear();

        log.info("LogViewerController cleanup completed");
    }
}
