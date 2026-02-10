package com.chatv2.client.gui.controller;

import com.chatv2.client.core.ChatClient;
import com.chatv2.client.core.ConnectionState;
import com.chatv2.client.discovery.ServerDiscovery;
import com.chatv2.client.discovery.ServerInfo;
import com.chatv2.client.gui.ChatClientApp;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Controller for server selection screen.
 * Handles server discovery and manual connection.
 */
public class ServerSelectionController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(ServerSelectionController.class);

    @FXML private ListView<ServerInfo> serverListView;
    @FXML private Button refreshButton;
    @FXML private Button connectButton;
    @FXML private RadioButton autoDiscoveryRadio;
    @FXML private RadioButton manualInputRadio;
    @FXML private TextField manualHostField;
    @FXML private TextField manualPortField;
    @FXML private VBox serverListContainer;
    @FXML private VBox manualInputContainer;
    @FXML private Button manualConnectButton;

    private ChatClient chatClient;
    private ServerDiscovery serverDiscovery;
    private ObservableList<ServerInfo> serverList;
    private Timer refreshTimer;
    private boolean buttonsGloballyEnabled = true;
    private final Object timerLock = new Object();
    private Thread connectionThread;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing ServerSelectionController");

        // Get app instance
        ChatClientApp app = ChatClientApp.getInstance();
        if (app != null) {
            chatClient = app.getChatClient();
            serverDiscovery = app.getServerDiscovery();
        }

        // Initialize server list
        serverList = FXCollections.observableArrayList();
        serverListView.setItems(serverList);

        // Custom list cell factory
        serverListView.setCellFactory(lv -> new ServerListCell());

        // Setup radio buttons
        ToggleGroup discoveryGroup = new ToggleGroup();
        autoDiscoveryRadio.setToggleGroup(discoveryGroup);
        manualInputRadio.setToggleGroup(discoveryGroup);
        autoDiscoveryRadio.setSelected(true);

        // Setup visibility
        serverListContainer.setVisible(true);
        manualInputContainer.setVisible(false);
        manualInputContainer.setManaged(false);

        // Setup radio button actions
        autoDiscoveryRadio.setOnAction(e -> handleDiscoveryModeChange());
        manualInputRadio.setOnAction(e -> handleDiscoveryModeChange());

        // Setup button actions
        refreshButton.setOnAction(e -> handleRefresh());
        connectButton.setOnAction(e -> handleConnect());
        manualConnectButton.setOnAction(e -> handleManualConnect());

        // Setup selection listener
        serverListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateConnectButton());

        // Setup manual input field listeners
        manualHostField.textProperty().addListener((obs, oldVal, newVal) -> {
            log.debug("Manual host field changed: '{}'", newVal);
            updateManualConnectButton();
        });

        manualPortField.textProperty().addListener((obs, oldVal, newVal) -> {
            log.debug("Manual port field changed: '{}'", newVal);
            updateManualConnectButton();
        });

        // Start server discovery
        startServerDiscovery();

        // Start auto-refresh timer
        startAutoRefresh();

        updateConnectButton();
        updateManualConnectButton();
    }

    /**
     * Handles discovery mode change.
     */
    private void handleDiscoveryModeChange() {
        boolean autoDiscovery = autoDiscoveryRadio.isSelected();
        serverListContainer.setVisible(autoDiscovery);
        serverListContainer.setManaged(autoDiscovery);
        manualInputContainer.setVisible(!autoDiscovery);
        manualInputContainer.setManaged(!autoDiscovery);

        // Включаем/отключаем manualConnectButton
        manualConnectButton.setVisible(!autoDiscovery);
        manualConnectButton.setManaged(!autoDiscovery);

        // Инвертируем логику для кнопки connectButton
        connectButton.setVisible(autoDiscovery);
        connectButton.setManaged(autoDiscovery);

        // Управляем таймером автообновления в зависимости от режима
        if (autoDiscovery) {
            startAutoRefresh();
        } else {
            stopAutoRefresh();
        }

        // Обновляем состояние кнопок при переключении режимов
        updateManualConnectButton();
    }

    /**
     * Starts server discovery.
     */
    private void startServerDiscovery() {
        if (serverDiscovery == null) {
            log.warn("Server discovery is not available");
            return;
        }

        serverDiscovery.startDiscovery().thenRun(() -> {
            log.info("Server discovery started");

            // Add listener for new servers
            serverDiscovery.addListener(this::handleServerDiscovered);

            // Initial refresh
            refreshServerList();
        }).exceptionally(ex -> {
            log.error("Failed to start server discovery", ex);
            Platform.runLater(() -> showError("Failed to start server discovery: " + ex.getMessage()));
            return null;
        });
    }

    /**
     * Handles newly discovered server.
     */
    private void handleServerDiscovered(ServerInfo serverInfo) {
        Platform.runLater(() -> {
            if (!serverList.contains(serverInfo)) {
                serverList.add(serverInfo);
                log.info("Added server to list: {}", serverInfo.serverName());
            }
        });
    }

    /**
     * Starts auto-refresh timer.
     */
    public void startAutoRefresh() {
        synchronized (timerLock) {
            // Check if timer is already running
            if (refreshTimer != null) {
                log.debug("Auto-refresh timer is already running");
                return;
            }

            log.debug("Starting auto-refresh timer");
            refreshTimer = new Timer("ServerRefreshTimer", true);
            refreshTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> refreshServerList());
                }
            }, 10000, 10000); // Refresh every 10 seconds
        }
    }

    /**
     * Stops auto-refresh timer.
     * Can be called when switching to manual mode or navigating to other screens.
     */
    public void stopAutoRefresh() {
        synchronized (timerLock) {
            if (refreshTimer != null) {
                log.debug("Stopping auto-refresh timer");
                refreshTimer.cancel();
                refreshTimer = null;
            }
        }
    }

    /**
     * Checks if auto-discovery mode is active.
     *
     * @return true if auto-discovery radio button is selected, false otherwise
     */
    public boolean isAutoDiscoveryMode() {
        return autoDiscoveryRadio != null && autoDiscoveryRadio.isSelected();
    }

    /**
     * Refreshes the server list.
     * Only executes in auto-discovery mode.
     */
    private void refreshServerList() {
        // Skip refresh in manual mode
        if (!autoDiscoveryRadio.isSelected()) {
            log.debug("Skipping refresh: manual mode is active");
            return;
        }

        if (serverDiscovery == null) {
            return;
        }

        log.debug("Refreshing server list");
        var servers = serverDiscovery.getDiscoveredServers();

        // Remove old servers
        serverList.removeIf(server -> !servers.contains(server) && !server.isRecent());

        // Add new servers
        for (ServerInfo server : servers) {
            if (!serverList.contains(server)) {
                serverList.add(server);
            }
        }
    }

    /**
     * Handles refresh button click.
     */
    @FXML
    private void handleRefresh() {
        refreshServerList();
    }

    /**
     * Handles connect button click.
     */
    @FXML
    private void handleConnect() {
        ServerInfo selectedServer = serverListView.getSelectionModel().getSelectedItem();
        if (selectedServer == null) {
            showError("Please select a server");
            return;
        }

        if (selectedServer.isFull()) {
            showError("Server is full");
            return;
        }

        connectToServer(selectedServer.address(), selectedServer.port());
    }

    /**
     * Handles manual connect button click.
     */
    @FXML
    private void handleManualConnect() {
        String host = manualHostField.getText().trim();
        String portText = manualPortField.getText().trim();

        if (host.isEmpty()) {
            showError("Please enter server address");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                showError("Port must be between 1 and 65535");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Invalid port number");
            return;
        }

        connectToServer(host, port);
    }

    /**
     * Connects to specified server.
     */
    private void connectToServer(String host, int port) {
        log.info("Connecting to server {}:{}", host, port);

        // Check if connection is already in progress
        if (connectionThread != null && connectionThread.isAlive()) {
            log.warn("Connection already in progress");
            return;
        }

        // Disable buttons
        setButtonsEnabled(false);

        // Connect in background
        connectionThread = new Thread(() -> {
            try {
                chatClient.connect(host, port).thenRun(() -> {
                    Platform.runLater(() -> {
                        log.info("Connected to server {}:{}", host, port);
                        ChatClientApp.getInstance().showLoginScene();
                    });
                }).exceptionally(ex -> {
                    Platform.runLater(() -> {
                        log.error("Failed to connect to server {}:{}", host, port, ex);
                        showError("Failed to connect: " + ex.getMessage());
                        setButtonsEnabled(true);
                    });
                    return null;
                }).join();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    log.error("Connection error", e);
                    showError("Connection error: " + e.getMessage());
                    setButtonsEnabled(true);
                });
            } finally {
                connectionThread = null;
            }
        }, "ConnectionThread");
        connectionThread.start();
    }

    /**
     * Updates connect button state.
     */
    private void updateConnectButton() {
        ServerInfo selected = serverListView.getSelectionModel().getSelectedItem();
        connectButton.setDisable(selected == null || !buttonsGloballyEnabled);
    }

    /**
     * Updates manual connect button state based on field values.
     */
    private void updateManualConnectButton() {
        String host = manualHostField.getText().trim();
        String portText = manualPortField.getText().trim();

        boolean isHostValid = !host.isEmpty();
        boolean isPortValid = false;

        if (!portText.isEmpty()) {
            try {
                int port = Integer.parseInt(portText);
                isPortValid = port >= 1 && port <= 65535;
                log.debug("Port validation: '{}' -> valid={}, value={}", portText, isPortValid, port);
            } catch (NumberFormatException e) {
                log.debug("Port validation: '{}' -> invalid (not a number)", portText);
                isPortValid = false;
            }
        }

        boolean shouldEnable = isHostValid && isPortValid && buttonsGloballyEnabled;
        manualConnectButton.setDisable(!shouldEnable);
        log.debug("Manual connect button state: hostValid={}, portValid={}, globallyEnabled={}, buttonEnabled={}",
                isHostValid, isPortValid, buttonsGloballyEnabled, shouldEnable);
    }

    /**
     * Enables/disables buttons.
     */
    private void setButtonsEnabled(boolean enabled) {
        log.debug("Setting buttons globally enabled: {}", enabled);
        buttonsGloballyEnabled = enabled;
        refreshButton.setDisable(!enabled);
        connectButton.setDisable(!enabled);
        updateConnectButton();
        updateManualConnectButton();
    }

    /**
     * Shows error alert.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Cleanup method to stop auto-refresh and interrupt connection thread.
     */
    public void cleanup() {
        stopAutoRefresh();
        if (connectionThread != null && connectionThread.isAlive()) {
            connectionThread.interrupt();
            try {
                connectionThread.join(1000); // Wait up to 1 second
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for connection thread to finish");
            }
            connectionThread = null;
        }
    }

    /**
     * Custom list cell for server display.
     */
    private static class ServerListCell extends ListCell<ServerInfo> {
        private final VBox content = new VBox(5);
        private final HBox headerBox = new HBox(10);
        private final Label nameLabel = new Label();
        private final Label statusLabel = new Label();
        private final Label detailsLabel = new Label();
        private final Circle statusIndicator = new Circle(5);

        public ServerListCell() {
            super();
            content.setPadding(new javafx.geometry.Insets(10, 10, 10, 10));

            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            statusLabel.setStyle("-fx-font-size: 12px;");
            detailsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #c0c0c0;");

            headerBox.getChildren().addAll(nameLabel, statusLabel, statusIndicator);
            content.getChildren().addAll(headerBox, detailsLabel);
        }

        @Override
        protected void updateItem(ServerInfo server, boolean empty) {
            super.updateItem(server, empty);

            if (empty || server == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(server.serverName());
                detailsLabel.setText(String.format("%s:%d | %s | %d/%d users",
                    server.address(), server.port(), server.version(),
                    server.currentUsers(), server.maxUsers()));

                // Status indicator
                if (server.isFull()) {
                    statusIndicator.setFill(Color.RED);
                    statusLabel.setText("Full");
                } else if (server.isRecent()) {
                    statusIndicator.setFill(Color.GREEN);
                    statusLabel.setText("Online");
                } else {
                    statusIndicator.setFill(Color.ORANGE);
                    statusLabel.setText("Offline");
                }

                setGraphic(content);
            }
        }
    }
}
