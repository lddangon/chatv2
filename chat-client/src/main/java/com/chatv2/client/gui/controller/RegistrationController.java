package com.chatv2.client.gui.controller;

import com.chatv2.client.core.ChatClient;
import com.chatv2.client.gui.ChatClientApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for registration screen.
 * Handles new user registration with avatar support.
 */
public class RegistrationController implements Initializable {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField fullNameField;
    @FXML private TextArea bioTextArea;
    @FXML private Button chooseAvatarButton;
    @FXML private ImageView avatarImageView;
    @FXML private Button registerButton;
    @FXML private Button backButton;
    @FXML private Label errorLabel;

    private ChatClient chatClient;
    private byte[] avatarData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing RegistrationController");

        // Get chat client
        ChatClientApp app = ChatClientApp.getInstance();
        if (app != null) {
            chatClient = app.getChatClient();
        }

        // Setup button actions
        chooseAvatarButton.setOnAction(e -> handleChooseAvatar());
        registerButton.setOnAction(e -> handleRegister());
        backButton.setOnAction(e -> handleBack());

        // Setup avatar image
        avatarImageView.setFitWidth(128);
        avatarImageView.setFitHeight(128);
        avatarImageView.setPreserveRatio(true);
        avatarImageView.setSmooth(true);

        // Limit bio length
        bioTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 500) {
                bioTextArea.setText(oldVal);
            }
        });

        clearError();
    }

    /**
     * Handles choose avatar button click.
     */
    @FXML
    private void handleChooseAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Avatar");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"),
            new FileChooser.ExtensionFilter("PNG Files", "*.png"),
            new FileChooser.ExtensionFilter("JPEG Files", "*.jpg", "*.jpeg"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(avatarImageView.getScene().getWindow());
        if (file != null) {
            try {
                // Check file size (max 1MB)
                if (file.length() > 1024 * 1024) {
                    showError("Avatar image must be less than 1MB");
                    return;
                }

                // Load image
                BufferedImage originalImage = ImageIO.read(file);
                if (originalImage == null) {
                    showError("Invalid image file");
                    return;
                }

                // Resize to 128x128
                BufferedImage resizedImage = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = resizedImage.createGraphics();
                g.drawImage(originalImage, 0, 0, 128, 128, null);
                g.dispose();

                // Convert to bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(resizedImage, "png", baos);
                avatarData = baos.toByteArray();

                // Display in ImageView
                Image image = new Image(file.toURI().toString());
                avatarImageView.setImage(image);

                log.info("Avatar loaded: {} (size: {} bytes)", file.getName(), avatarData.length);

            } catch (Exception e) {
                log.error("Failed to load avatar", e);
                showError("Failed to load avatar: " + e.getMessage());
            }
        }
    }

    /**
     * Handles register button click.
     */
    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String fullName = fullNameField.getText().trim();
        String bio = bioTextArea.getText().trim();

        // Validate inputs
        if (!validateInputs(username, password, confirmPassword, fullName, bio)) {
            return;
        }

        log.info("Attempting registration for user: {}", username);

        // Disable button and show loading
        registerButton.setDisable(true);
        errorLabel.setText("Registering...");

        // Perform registration in background
        new Thread(() -> {
            try {
                chatClient.register(username, password, fullName).thenRun(() -> {
                    Platform.runLater(() -> {
                        log.info("Registration successful for user: {}", username);
                        showAlert("Registration Successful", "Your account has been created. Please login.");
                        ChatClientApp.getInstance().showLoginScene();
                    });
                }).exceptionally(ex -> {
                    Platform.runLater(() -> {
                        log.error("Registration failed for user: {}", username, ex);
                        showError("Registration failed: " + ex.getMessage());
                        registerButton.setDisable(false);
                    });
                    return null;
                }).join();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    log.error("Registration error", e);
                    showError("Registration error: " + e.getMessage());
                    registerButton.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * Handles back button click.
     */
    @FXML
    private void handleBack() {
        log.info("Navigating back to login");
        ChatClientApp.getInstance().showLoginScene();
    }

    /**
     * Validates registration inputs.
     */
    private boolean validateInputs(String username, String password, String confirmPassword,
                                   String fullName, String bio) {
        // Username validation
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

        // Password validation
        if (password.isEmpty()) {
            showError("Please enter password");
            return false;
        }
        if (password.length() < 8) {
            showError("Password must be at least 8 characters");
            return false;
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
            showError("Password must contain both letters and numbers");
            return false;
        }

        // Confirm password validation
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return false;
        }

        // Full name validation
        if (fullName.isEmpty()) {
            showError("Please enter your full name");
            return false;
        }
        if (fullName.length() < 2) {
            showError("Full name must be at least 2 characters");
            return false;
        }
        if (fullName.length() > 100) {
            showError("Full name must not exceed 100 characters");
            return false;
        }

        // Bio validation
        if (bio.length() > 500) {
            showError("Bio must not exceed 500 characters");
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
     * Shows alert dialog.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegistrationController.class);
}
