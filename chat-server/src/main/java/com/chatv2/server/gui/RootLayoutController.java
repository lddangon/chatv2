package com.chatv2.server.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the root layout with navigation sidebar.
 * Handles navigation between different views.
 */
public class RootLayoutController {
    private static final Logger log = LoggerFactory.getLogger(RootLayoutController.class);

    private ServerAdminApp mainApp;

    @FXML private Button dashboardButton;
    @FXML private Button userManagementButton;
    @FXML private Button chatManagementButton;
    @FXML private Button logViewerButton;

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
     */
    @FXML
    private void initialize() {
        log.debug("Initializing RootLayoutController");
    }

    /**
     * Handles Dashboard button click.
     */
    @FXML
    private void handleDashboard() {
        log.debug("Navigating to Dashboard");
        if (mainApp != null) {
            mainApp.showDashboardView();
        }
    }

    /**
     * Handles User Management button click.
     */
    @FXML
    private void handleUserManagement() {
        log.debug("Navigating to User Management");
        if (mainApp != null) {
            mainApp.showUserManagementView();
        }
    }

    /**
     * Handles Chat Management button click.
     */
    @FXML
    private void handleChatManagement() {
        log.debug("Navigating to Chat Management");
        if (mainApp != null) {
            mainApp.showChatManagementView();
        }
    }

    /**
     * Handles Log Viewer button click.
     */
    @FXML
    private void handleLogViewer() {
        log.debug("Navigating to Log Viewer");
        if (mainApp != null) {
            mainApp.showLogViewerView();
        }
    }
}
