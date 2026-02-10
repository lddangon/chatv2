package com.chatv2.client.gui;

import com.chatv2.client.core.ChatClient;
import com.chatv2.client.core.ClientConfig;
import com.chatv2.client.core.ConnectionState;
import com.chatv2.client.discovery.ServerDiscovery;
import com.chatv2.client.gui.controller.ServerSelectionController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Main JavaFX application for ChatV2 Client.
 * Manages all scenes and application lifecycle.
 */
public class ChatClientApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(ChatClientApp.class);
    private static ChatClientApp instance;

    private ChatClient chatClient;
    private ServerDiscovery serverDiscovery;
    private ClientConfig config;

    private Stage primaryStage;
    private Scene serverSelectionScene;
    private Scene loginScene;
    private Scene registrationScene;
    private Scene chatScene;
    private Scene profileScene;

    private ServerSelectionController serverSelectionController;

    /**
     * Gets the singleton instance of the application.
     */
    public static ChatClientApp getInstance() {
        return instance;
    }

    @Override
    public void init() throws Exception {
        log.info("Initializing ChatV2 Client GUI");
        instance = this;

        // Load configuration
        config = loadConfig();

        // Initialize chat client
        chatClient = new ChatClient(config);

        // Initialize server discovery
        if (config.discoveryEnabled()) {
            serverDiscovery = new ServerDiscovery(
                config.udpMulticastAddress(),
                config.udpMulticastPort(),
                config.discoveryTimeoutSeconds()
            );
        }

        log.info("ChatV2 Client GUI initialized");
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        log.info("Starting ChatV2 Client GUI");

        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("ChatV2 Client");

        // Load CSS
        String css = Objects.requireNonNull(getClass().getResource("/css/client.css")).toExternalForm();

        // Load all scenes
        loadScenes(css);

        // Set default scene
        this.primaryStage.setScene(serverSelectionScene);
        this.primaryStage.setMinWidth(800);
        this.primaryStage.setMinHeight(600);
        this.primaryStage.setResizable(true);

        // Set app icon
        try {
            primaryStage.getIcons().add(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/images/chat-icon.png"))));
        } catch (Exception e) {
            log.warn("Could not load app icon", e);
        }

        // Handle window close
        this.primaryStage.setOnCloseRequest(event -> {
            event.consume();
            handleShutdown();
        });

        this.primaryStage.show();
        log.info("ChatV2 Client GUI started");
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping ChatV2 Client GUI");
        if (serverSelectionController != null) {
            serverSelectionController.stopAutoRefresh();
        }
        shutdown();
    }

    /**
     * Loads all application scenes.
     */
    private void loadScenes(String css) throws IOException {
        // Server Selection Scene
        FXMLLoader serverSelectionLoader = new FXMLLoader(
            getClass().getResource("/fxml/ServerSelectionView.fxml"));
        Parent serverSelectionRoot = serverSelectionLoader.load();
        serverSelectionScene = new Scene(serverSelectionRoot, 900, 700);
        serverSelectionScene.getStylesheets().add(css);
        serverSelectionController = serverSelectionLoader.getController();

        // Login Scene
        FXMLLoader loginLoader = new FXMLLoader(
            getClass().getResource("/fxml/LoginView.fxml"));
        Parent loginRoot = loginLoader.load();
        loginScene = new Scene(loginRoot, 450, 550);
        loginScene.getStylesheets().add(css);

        // Registration Scene
        FXMLLoader registrationLoader = new FXMLLoader(
            getClass().getResource("/fxml/RegistrationView.fxml"));
        Parent registrationRoot = registrationLoader.load();
        registrationScene = new Scene(registrationRoot, 500, 650);
        registrationScene.getStylesheets().add(css);

        // Chat Scene
        FXMLLoader chatLoader = new FXMLLoader(
            getClass().getResource("/fxml/ChatView.fxml"));
        Parent chatRoot = chatLoader.load();
        chatScene = new Scene(chatRoot, 1200, 800);
        chatScene.getStylesheets().add(css);

        // Profile Scene
        FXMLLoader profileLoader = new FXMLLoader(
            getClass().getResource("/fxml/ProfileView.fxml"));
        Parent profileRoot = profileLoader.load();
        profileScene = new Scene(profileRoot, 600, 500);
        profileScene.getStylesheets().add(css);
    }

    /**
     * Shows the server selection scene.
     */
    public void showServerSelectionScene() {
        Platform.runLater(() -> {
            if (serverSelectionController != null && serverSelectionController.isAutoDiscoveryMode()) {
                serverSelectionController.startAutoRefresh();
            }
            primaryStage.setScene(serverSelectionScene);
            primaryStage.setTitle("ChatV2 Client - Select Server");
        });
    }

    /**
     * Shows the login scene.
     */
    public void showLoginScene() {
        if (serverSelectionController != null) {
            serverSelectionController.stopAutoRefresh();
        }
        Platform.runLater(() -> {
            primaryStage.setScene(loginScene);
            primaryStage.setTitle("ChatV2 Client - Login");
        });
    }

    /**
     * Shows the registration scene.
     */
    public void showRegistrationScene() {
        if (serverSelectionController != null) {
            serverSelectionController.stopAutoRefresh();
        }
        Platform.runLater(() -> {
            primaryStage.setScene(registrationScene);
            primaryStage.setTitle("ChatV2 Client - Register");
        });
    }

    /**
     * Shows the chat scene.
     */
    public void showChatScene() {
        Platform.runLater(() -> {
            primaryStage.setScene(chatScene);
            primaryStage.setTitle("ChatV2 Client - Chat");
        });
    }

    /**
     * Shows the profile scene.
     */
    public void showProfileScene() {
        Platform.runLater(() -> {
            primaryStage.setScene(profileScene);
            primaryStage.setTitle("ChatV2 Client - Profile");
        });
    }

    /**
     * Handles application shutdown.
     */
    private void handleShutdown() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Exit");
        alert.setHeaderText("Do you want to exit ChatV2 Client?");
        alert.setContentText("All unsaved data will be lost.");

        alert.showAndWait().ifPresent(response -> {
            if (response.getButtonData().isDefaultButton()) {
                shutdown();
                Platform.exit();
                System.exit(0);
            }
        });
    }

    /**
     * Shuts down the application gracefully.
     */
    public void shutdown() {
        log.info("Shutting down ChatV2 Client");

        try {
            // Cleanup controller first
            if (serverSelectionController != null) {
                serverSelectionController.cleanup();
            }
            
            if (serverDiscovery != null) {
                serverDiscovery.shutdown().join();
            }
            if (chatClient != null) {
                chatClient.shutdown().join();
            }
        } catch (Exception e) {
            log.error("Error during shutdown", e);
        }
    }

    /**
     * Loads client configuration.
     */
    private ClientConfig loadConfig() {
        return new ClientConfig(
            "ChatV2 Client",
            "1.0.0",
            true,
            "239.255.255.250",
            9999,
            30,
            5,
            5,
            30,
            true
        );
    }

    /**
     * Gets the chat client instance.
     */
    public ChatClient getChatClient() {
        return chatClient;
    }

    /**
     * Gets the server discovery instance.
     */
    public ServerDiscovery getServerDiscovery() {
        return serverDiscovery;
    }

    /**
     * Gets the client configuration.
     */
    public ClientConfig getConfig() {
        return config;
    }

    /**
     * Gets the primary stage.
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
