package com.chatv2.client.gui.controller;

import com.chatv2.client.core.ChatClient;
import com.chatv2.client.core.CredentialStorage;
import com.chatv2.client.gui.ChatClientApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for login screen.
 * Handles user authentication.
 */
public class LoginController implements Initializable {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Button backButton;
    @FXML private Label errorLabel;

    private ChatClient chatClient;
    private final CredentialStorage credentialStorage = new CredentialStorage();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing LoginController");

        // Get chat client
        ChatClientApp app = ChatClientApp.getInstance();
        if (app != null) {
            chatClient = app.getChatClient();
        }

        // Setup button actions
        loginButton.setOnAction(e -> handleLogin());
        registerButton.setOnAction(e -> handleRegister());
        backButton.setOnAction(e -> handleBack());

        // Setup enter key for login
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                handleLogin();
            }
        });

        // Load saved credentials if remember me is checked
        loadSavedCredentials();

        clearError();
    }

    /**
     * Handles login button click.
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        boolean rememberMe = rememberMeCheckBox.isSelected();

        // Validate inputs
        if (!validateInputs(username, password)) {
            return;
        }

        log.info("Attempting login for user: {}", username);

        // Disable button and show loading
        loginButton.setDisable(true);
        errorLabel.setText("Logging in...");

        // Perform login in background
        new Thread(() -> {
            try {
                chatClient.login(username, password).thenRun(() -> {
                    Platform.runLater(() -> {
                        log.info("Login successful for user: {}", username);
                        // Save credentials if remember me is checked
                        saveCredentials(username, password, rememberMe);
                        ChatClientApp.getInstance().showChatScene();
                    });
                }).exceptionally(ex -> {
                    Platform.runLater(() -> {
                        log.error("Login failed for user: {}", username, ex);
                        showError("Login failed: " + ex.getMessage());
                        loginButton.setDisable(false);
                    });
                    return null;
                }).join();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    log.error("Login error", e);
                    showError("Login error: " + e.getMessage());
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * Handles register button click.
     */
    @FXML
    private void handleRegister() {
        log.info("Navigating to registration");
        ChatClientApp.getInstance().showRegistrationScene();
    }

    /**
     * Handles back button click.
     */
    @FXML
    private void handleBack() {
        log.info("Navigating back to server selection");

        // Disconnect from server
        if (chatClient != null) {
            chatClient.disconnect().thenRun(() -> {
                Platform.runLater(() -> ChatClientApp.getInstance().showServerSelectionScene());
            }).exceptionally(ex -> {
                log.warn("Error disconnecting", ex);
                Platform.runLater(() -> ChatClientApp.getInstance().showServerSelectionScene());
                return null;
            });
        } else {
            ChatClientApp.getInstance().showServerSelectionScene();
        }
    }

    /**
     * Validates login inputs.
     */
    private boolean validateInputs(String username, String password) {
        if (username.isEmpty()) {
            showError("Please enter username");
            return false;
        }

        if (username.length() < 3) {
            showError("Username must be at least 3 characters");
            return false;
        }

        if (username.length() > 20) {
            showError("Username must not exceed 20 characters");
            return false;
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            showError("Username can only contain letters, numbers, and underscores");
            return false;
        }

        if (password.isEmpty()) {
            showError("Please enter password");
            return false;
        }

        if (password.length() < 8) {
            showError("Password must be at least 8 characters");
            return false;
        }

        clearError();
        return true;
    }

    /**
     * Shows error message.
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle("-fx-text-fill: red;");
    }

    /**
     * Clears error message.
     */
    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    /**
     * Loads saved credentials from storage.
     */
    private void loadSavedCredentials() {
        try {
            Map<String, String> credentials = credentialStorage.loadCredentials();
            if (!credentials.isEmpty()) {
                String username = credentials.get(CredentialStorage.KEY_USERNAME);
                String password = credentials.get(CredentialStorage.KEY_PASSWORD);

                if (username != null && !username.isBlank()) {
                    usernameField.setText(username);
                    rememberMeCheckBox.setSelected(true);

                    // Only auto-fill password if user explicitly chooses
                    // For security, we don't auto-fill password
                    log.debug("Loaded saved username: {}", username);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load saved credentials", e);
        }
    }

    /**
     * Saves credentials to storage if remember me is checked.
     */
    private void saveCredentials(String username, String password, boolean rememberMe) {
        try {
            String serverHost = chatClient != null ? chatClient.getConnectedServerHost() : "";
            int serverPort = chatClient != null ? chatClient.getConnectedServerPort() : -1;
            credentialStorage.storeCredentials(username, password, rememberMe, serverHost, serverPort);
        } catch (Exception e) {
            log.error("Failed to save credentials", e);
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoginController.class);
}
