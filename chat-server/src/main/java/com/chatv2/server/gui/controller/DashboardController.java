package com.chatv2.server.gui.controller;

import com.chatv2.common.model.UserProfile;
import com.chatv2.server.core.ChatServer;
import com.chatv2.server.gui.ServerAdminApp;
import com.chatv2.server.gui.model.ServerStatistics;
import com.chatv2.server.manager.UserManager;
import com.chatv2.server.storage.DatabaseManager;
import com.chatv2.server.manager.ChatManager;
import com.chatv2.server.manager.ChatManager;
import com.chatv2.server.manager.MessageManager;
import com.chatv2.server.storage.UserRepository;
import com.chatv2.server.storage.ChatRepository;
import com.chatv2.server.storage.MessageRepository;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller for the Dashboard view.
 * Displays server statistics, activity charts, and server control buttons.
 */
public class DashboardController {
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private static final int MAX_CHART_POINTS = 20;

    private ServerAdminApp mainApp;
    private ScheduledExecutorService updateScheduler;

    // Statistics labels
    @FXML private Label serverNameLabel;
    @FXML private Label serverStatusLabel;
    @FXML private Label uptimeLabel;
    @FXML private Label connectedClientsLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalChatsLabel;
    @FXML private Label totalMessagesLabel;
    @FXML private Label messagesTodayLabel;

    // Control buttons
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button restartButton;

    // Charts
    @FXML private LineChart<String, Number> activityChart;

    // Statistics container
    @FXML private GridPane statisticsGrid;

    // Data series for charts
    private XYChart.Series<String, Number> messageSeries;
    private XYChart.Series<String, Number> connectionSeries;

    // Message count for "today"
    private int messagesToday = 0;
    private Instant lastResetTime = Instant.now();

    /**
     * Sets the main application instance.
     *
     * @param mainApp the ServerAdminApp instance
     */
    public void setMainApp(ServerAdminApp mainApp) {
        this.mainApp = mainApp;
    }

    /**
     * Initializes the controller.
     * Sets up charts and starts periodic updates.
     */
    @FXML
    private void initialize() {
        log.debug("Initializing DashboardController");

        // Initialize chart series
        messageSeries = new XYChart.Series<>();
        messageSeries.setName("Messages");
        connectionSeries = new XYChart.Series<>();
        connectionSeries.setName("Connections");

        activityChart.getData().add(messageSeries);
        activityChart.getData().add(connectionSeries);

        // Set chart styling
        activityChart.setCreateSymbols(false);
        activityChart.setLegendVisible(true);

        // Start periodic updates (every 5 seconds)
        updateScheduler = Executors.newSingleThreadScheduledExecutor();
        updateScheduler.scheduleAtFixedRate(this::updateStatistics, 0, 5, TimeUnit.SECONDS);

        // Update message count reset at midnight
        updateScheduler.scheduleAtFixedRate(this::checkMidnightReset, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Updates the dashboard statistics.
     */
    private void updateStatistics() {
        Task<Void> updateTask = new Task<>() {
            @Override
            protected Void call() {
                try {
                    // Check if ServerAdminApp is properly initialized
                    if (!ServerAdminApp.isInitialized() || mainApp == null) {
                        log.warn("ServerAdminApp or mainApp not initialized yet, skipping update");
                        return null;
                    }

                    ChatServer server = ServerAdminApp.getChatServer();
                    if (server == null) {
                        return null;
                    }

                    // Get database manager safely
                    DatabaseManager dbManager = ServerAdminApp.getDatabaseManager();
                    if (dbManager == null) {
                        log.warn("DatabaseManager is null, skipping update");
                        return null;
                    }

                    UserManager userManager = dbManager.getUserManager();
                    ChatManager chatManager = dbManager.getChatManager();
                    MessageManager messageManager = dbManager.getMessageManager();
                    UserRepository userRepository = dbManager.getUserRepository();
                    ChatRepository chatRepository = dbManager.getChatRepository();
                    MessageRepository messageRepository = dbManager.getMessageRepository();

                    // Gather statistics
                    int userCount = userRepository.countAll();
                    int activeSessions = server.getConnectedClients();
                    int chatCount = chatRepository.countAll();
                    int messageCount = messageRepository.countAll();

                    // Update messages today (simple approximation)
                    List<UUID> allUserIds = userRepository.findAll().stream().map(UserProfile::userId).toList();
                    int todayMessages = 0;
                    try {
                        for (UUID userId : allUserIds) {
                            // Count messages sent after last reset time
                            todayMessages += messageRepository.countByUserAfterDate(userId, lastResetTime);
                        }
                    } catch (Exception e) {
                        log.warn("Could not count today's messages", e);
                    }

                    long uptimeSeconds = server.getUptimeSeconds();
                    Duration uptime = Duration.ofSeconds(uptimeSeconds);

                    ServerStatistics stats = new ServerStatistics(
                        userCount, activeSessions, chatCount, messageCount, todayMessages, uptime
                    );

                    // Update UI on JavaFX thread
                    Platform.runLater(() -> updateUI(stats));

                } catch (Exception e) {
                    log.error("Error updating statistics", e);
                }
                return null;
            }
        };

        new Thread(updateTask).start();
    }

    /**
     * Updates the UI with new statistics.
     *
     * @param stats the server statistics
     */
    private void updateUI(ServerStatistics stats) {
        // Defensive check: Verify initialization before accessing dependencies
        if (!ServerAdminApp.isInitialized()) {
            log.warn("ServerAdminApp not initialized yet, skipping UI update");
            return;
        }

        ChatServer server = ServerAdminApp.getChatServer();

        // Update server info
        serverNameLabel.setText(ServerAdminApp.getServerConfig().getName());
        serverStatusLabel.setText(server.getState().name());

        // Update statistics
        uptimeLabel.setText(stats.getFormattedUptime());
        connectedClientsLabel.setText(String.valueOf(stats.activeSessions()));
        totalUsersLabel.setText(String.valueOf(stats.userCount()));
        totalChatsLabel.setText(String.valueOf(stats.chatCount()));
        totalMessagesLabel.setText(String.valueOf(stats.messageCount()));
        messagesTodayLabel.setText(String.valueOf(stats.messagesToday()));

        // Update control buttons state
        boolean isRunning = server.getState() == ChatServer.ServerState.RUNNING;
        startButton.setDisable(isRunning);
        stopButton.setDisable(!isRunning);
        restartButton.setDisable(!isRunning);

        // Update charts
        String timeLabel = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        messageSeries.getData().add(new XYChart.Data<>(timeLabel, stats.messagesToday()));
        connectionSeries.getData().add(new XYChart.Data<>(timeLabel, stats.activeSessions()));

        // Limit chart data to last 20 points
        if (messageSeries.getData().size() > MAX_CHART_POINTS) {
            messageSeries.getData().remove(0);
            connectionSeries.getData().remove(0);
        }
    }

    /**
     * Checks if it's midnight and resets message count.
     */
    private void checkMidnightReset() {
        LocalDateTime now = LocalDateTime.now();
        if (now.getHour() == 0 && now.getMinute() == 0 && now.getSecond() < 10) {
            lastResetTime = Instant.now();
            messagesToday = 0;
            log.info("Message count reset at midnight");
        }
    }

    /**
     * Handles Start Server button click.
     */
    @FXML
    private void handleStartServer() {
        log.info("Start Server button clicked");
        
        // Defensive check: Verify initialization before accessing dependencies
        if (!ServerAdminApp.isInitialized()) {
            log.warn("ServerAdminApp not initialized yet, cannot start server");
            mainApp.showErrorAlert(
                "Initialization Error",
                "Server Not Ready",
                "The server is still initializing. Please wait a moment and try again."
            );
            return;
        }
        
        ChatServer server = ServerAdminApp.getChatServer();

        server.start().thenAccept(v -> {
            Platform.runLater(() -> {
                mainApp.showInfoAlert(
                    "Server Started",
                    "Server started successfully",
                    "Server is now running on " + ServerAdminApp.getServerConfig().getHost() +
                        ":" + ServerAdminApp.getServerConfig().getPort()
                );
                updateStatistics();
            });
        }).exceptionally(ex -> {
            Platform.runLater(() ->
                mainApp.showErrorAlert("Start Failed", "Failed to start server", ex.getCause().getMessage())
            );
            return null;
        });
    }

    /**
     * Handles Stop Server button click.
     */
    @FXML
    private void handleStopServer() {
        log.info("Stop Server button clicked");
        
        // Defensive check: Verify initialization before accessing dependencies
        if (!ServerAdminApp.isInitialized()) {
            log.warn("ServerAdminApp not initialized yet, cannot stop server");
            mainApp.showErrorAlert(
                "Initialization Error",
                "Server Not Ready",
                "The server is still initializing. Please wait a moment and try again."
            );
            return;
        }
        
        ChatServer server = ServerAdminApp.getChatServer();

        server.stop().thenAccept(v -> {
            Platform.runLater(() -> {
                mainApp.showInfoAlert(
                    "Server Stopped",
                    "Server stopped successfully",
                    "The server has been stopped."
                );
                updateStatistics();
            });
        }).exceptionally(ex -> {
            Platform.runLater(() ->
                mainApp.showErrorAlert("Stop Failed", "Failed to stop server", ex.getCause().getMessage())
            );
            return null;
        });
    }

    /**
     * Handles Restart Server button click.
     */
    @FXML
    private void handleRestartServer() {
        log.info("Restart Server button clicked");
        
        // Defensive check: Verify initialization before accessing dependencies
        if (!ServerAdminApp.isInitialized()) {
            log.warn("ServerAdminApp not initialized yet, cannot restart server");
            mainApp.showErrorAlert(
                "Initialization Error",
                "Server Not Ready",
                "The server is still initializing. Please wait a moment and try again."
            );
            return;
        }
        
        ChatServer server = ServerAdminApp.getChatServer();

        server.stop()
            .thenCompose(v -> server.start())
            .thenAccept(v -> {
                Platform.runLater(() -> {
                    mainApp.showInfoAlert(
                        "Server Restarted",
                        "Server restarted successfully",
                        "Server is running again."
                    );
                    updateStatistics();
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() ->
                    mainApp.showErrorAlert("Restart Failed", "Failed to restart server", ex.getCause().getMessage())
                );
                return null;
            });
    }

    /**
     * Cleans up resources when controller is destroyed.
     */
    public void cleanup() {
        log.debug("Cleaning up DashboardController");
        if (updateScheduler != null) {
            updateScheduler.shutdown();
        }
    }
}
