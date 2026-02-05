package com.chatv2.server.gui;

import com.chatv2.server.core.ChatServer;
import com.chatv2.server.core.ServerConfig;
import com.chatv2.server.handler.ServerInitializer;
import com.chatv2.server.storage.DatabaseManager;
import com.chatv2.server.gui.controller.ChatController;
import com.chatv2.server.gui.controller.DashboardController;
import com.chatv2.server.gui.controller.LogViewerController;
import com.chatv2.server.gui.controller.UserController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * JavaFX main application class for ChatV2 Server Admin GUI.
 * Provides a graphical interface for server administration.
 */
public class ServerAdminApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(ServerAdminApp.class);

    private static ChatServer chatServer;
    private static DatabaseManager databaseManager;
    private static ServerConfig serverConfig;
    private static boolean headless = false;

    private Stage primaryStage;
    private BorderPane rootLayout;

    /**
     * Main entry point for the GUI application.
     *
     * @param args command line arguments (--no-gui for headless mode)
     */
    public static void main(String[] args) {
        log.info("Starting ChatV2 Server Admin GUI...");
        log.info("Java Version: {}", System.getProperty("java.version"));

        // Parse command line arguments
        for (String arg : args) {
            if ("--no-gui".equals(arg)) {
                headless = true;
                break;
            }
        }

        if (headless) {
            log.info("Running in headless mode (GUI disabled)");
            // Headless mode - return immediately, let ServerLauncher handle it
            return;
        }

        // Launch JavaFX application
        launch(args);
    }

    /**
     * Sets the ChatServer instance for GUI integration.
     * Called from ServerLauncher before launching GUI.
     *
     * @param server the ChatServer instance
     */
    public static void setChatServer(ChatServer server) {
        chatServer = server;
    }

    /**
     * Sets the DatabaseManager instance for GUI integration.
     *
     * @param dbManager the DatabaseManager instance
     */
    public static void setDatabaseManager(DatabaseManager dbManager) {
        databaseManager = dbManager;
    }

    /**
     * Sets the ServerConfig instance.
     *
     * @param config the ServerConfig instance
     */
    public static void setServerConfig(ServerConfig config) {
        serverConfig = config;
    }

    /**
     * Gets the ChatServer instance.
     *
     * @return the ChatServer instance
     */
    public static ChatServer getChatServer() {
        return chatServer;
    }

    /**
     * Gets the DatabaseManager instance.
     *
     * @return the DatabaseManager instance
     */
    public static DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Gets the ServerConfig instance.
     *
     * @return the ServerConfig instance
     */
    public static ServerConfig getServerConfig() {
        return serverConfig;
    }

    @Override
    public void init() throws Exception {
        log.debug("Initializing ServerAdminApp");

        // Validate that dependencies are set
        if (chatServer == null) {
            throw new IllegalStateException("ChatServer instance not set. Call setChatServer() before launching GUI.");
        }
        if (databaseManager == null) {
            throw new IllegalStateException("DatabaseManager instance not set. Call setDatabaseManager() before launching GUI.");
        }
        if (serverConfig == null) {
            throw new IllegalStateException("ServerConfig instance not set. Call setServerConfig() before launching GUI.");
        }
    }

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting ServerAdmin GUI");

        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("ChatV2 Server Admin - " + serverConfig.getName());

        // Set application icon
        try {
            Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
            this.primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            log.warn("Could not load application icon", e);
        }

        // Load root layout
        initRootLayout();

        // Load default scene (Dashboard)
        showDashboardView();

        // Set minimum window size
        this.primaryStage.setMinWidth(1200);
        this.primaryStage.setMinHeight(800);

        // Handle window close request
        this.primaryStage.setOnCloseRequest(event -> {
            event.consume();
            handleExit();
        });

        this.primaryStage.show();
        log.info("ServerAdmin GUI started successfully");
    }

    @Override
    public void stop() throws Exception {
        log.debug("Stopping ServerAdminApp");
        // ChatServer shutdown is handled by ServerLauncher
    }

    /**
     * Initializes the root layout with navigation sidebar.
     */
    private void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/RootLayout.fxml"));
            rootLayout = loader.load();

            // Apply CSS stylesheet
            Scene scene = new Scene(rootLayout);
            scene.getStylesheets().add(getClass().getResource("/css/server-admin.css").toExternalForm());

            primaryStage.setScene(scene);

            // Get controller and set primary stage
            RootLayoutController controller = loader.getController();
            if (controller != null) {
                controller.setMainApp(this);
            }

        } catch (IOException e) {
            log.error("Failed to load root layout", e);
            showErrorAlert("Initialization Error", "Failed to load application layout", e.getMessage());
        }
    }

    /**
     * Shows the Dashboard view.
     */
    public void showDashboardView() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/DashboardView.fxml"));
            BorderPane dashboardView = loader.load();

            rootLayout.setCenter(dashboardView);

            DashboardController controller = loader.getController();
            if (controller != null) {
                controller.setMainApp(this);
            }

        } catch (IOException e) {
            log.error("Failed to load Dashboard view", e);
            showErrorAlert("View Error", "Failed to load Dashboard", e.getMessage());
        }
    }

    /**
     * Shows the User Management view.
     */
    public void showUserManagementView() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/UserManagementView.fxml"));
            BorderPane userManagementView = loader.load();

            rootLayout.setCenter(userManagementView);

            UserController controller = loader.getController();
            if (controller != null) {
                controller.setMainApp(this);
            }

        } catch (IOException e) {
            log.error("Failed to load User Management view", e);
            showErrorAlert("View Error", "Failed to load User Management", e.getMessage());
        }
    }

    /**
     * Shows the Chat Management view.
     */
    public void showChatManagementView() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/ChatManagementView.fxml"));
            BorderPane chatManagementView = loader.load();

            rootLayout.setCenter(chatManagementView);

            ChatController controller = loader.getController();
            if (controller != null) {
                controller.setMainApp(this);
            }

        } catch (IOException e) {
            log.error("Failed to load Chat Management view", e);
            showErrorAlert("View Error", "Failed to load Chat Management", e.getMessage());
        }
    }

    /**
     * Shows the Log Viewer view.
     */
    public void showLogViewerView() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/LogViewerView.fxml"));
            BorderPane logViewerView = loader.load();

            rootLayout.setCenter(logViewerView);

            LogViewerController controller = loader.getController();
            if (controller != null) {
                controller.setMainApp(this);
            }

        } catch (IOException e) {
            log.error("Failed to load Log Viewer view", e);
            showErrorAlert("View Error", "Failed to load Log Viewer", e.getMessage());
        }
    }

    /**
     * Handles application exit with confirmation dialog.
     */
    private void handleExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Application");
        alert.setHeaderText("Close ChatV2 Server Admin?");
        alert.setContentText("Are you sure you want to close the admin interface? The server will continue running.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            primaryStage.close();
        }
    }

    /**
     * Shows an error alert dialog.
     *
     * @param title   the alert title
     * @param header  the alert header text
     * @param content the alert content text
     */
    public void showErrorAlert(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /**
     * Shows an information alert dialog.
     *
     * @param title   the alert title
     * @param header  the alert header text
     * @param content the alert content text
     */
    public void showInfoAlert(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /**
     * Gets the primary stage.
     *
     * @return the primary stage
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
