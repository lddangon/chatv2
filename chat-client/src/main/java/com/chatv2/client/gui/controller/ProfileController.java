package com.chatv2.client.gui.controller;

import com.chatv2.client.core.ChatClient;
import com.chatv2.client.gui.ChatClientApp;
import com.chatv2.common.model.UserProfile;
import com.chatv2.common.model.UserStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Controller for profile screen.
 * Handles user profile viewing and editing.
 */
public class ProfileController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    @FXML private ImageView avatarImageView;
    @FXML private Label usernameLabel;
    @FXML private TextField fullNameField;
    @FXML private TextArea bioTextArea;
    @FXML private ComboBox<UserStatus> statusComboBox;
    @FXML private Button saveButton;
    @FXML private Button changeAvatarButton;
    @FXML private Button backButton;

    private ChatClient chatClient;
    private byte[] avatarData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing ProfileController");

        // Get chat client
        ChatClientApp app = ChatClientApp.getInstance();
        if (app != null) {
            chatClient = app.getChatClient();
        }

        // Setup status combo box
        statusComboBox.getItems().addAll(UserStatus.values());
        statusComboBox.getSelectionModel().selectFirst();

        // Setup button actions
        changeAvatarButton.setOnAction(e -> handleChangeAvatar());
        saveButton.setOnAction(e -> handleSave());
        backButton.setOnAction(e -> handleBack());

        // Setup avatar
        avatarImageView.setFitWidth(128);
        avatarImageView.setFitHeight(128);
        avatarImageView.setPreserveRatio(true);
        avatarImageView.setSmooth(true);
        avatarImageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");

        // Limit bio length
        bioTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 500) {
                bioTextArea.setText(oldVal);
            }
        });

        // Load profile data
        loadProfile();

        log.info("ProfileController initialized");
    }

    /**
     * Loads profile data.
     */
    private void loadProfile() {
        if (chatClient != null && chatClient.isAuthenticated()) {
            new Thread(() -> {
                try {
                    com.chatv2.common.model.UserProfile profile = chatClient.getProfile();
                    if (profile != null) {
                        Platform.runLater(() -> {
                            usernameLabel.setText(profile.username());
                            fullNameField.setText(profile.fullName());
                            bioTextArea.setText(profile.bio());
                            statusComboBox.getSelectionModel().select(profile.status());
                            
                            // Load avatar if available
                            if (profile.avatarData() != null && !profile.avatarData().isBlank()) {
                                try {
                                    byte[] avatarBytes = java.util.Base64.getDecoder().decode(profile.avatarData());
                                    javafx.scene.image.Image image = new javafx.scene.image.Image(
                                        new java.io.ByteArrayInputStream(avatarBytes)
                                    );
                                    avatarImageView.setImage(image);
                                } catch (Exception e) {
                                    log.warn("Failed to load avatar image", e);
                                }
                            }
                        });
                        log.info("Profile loaded successfully");
                    }
                } catch (Exception e) {
                    log.error("Failed to load profile", e);
                    Platform.runLater(() -> {
                        // Set default values on error
                        usernameLabel.setText("Username");
                        fullNameField.setText("John Doe");
                        bioTextArea.setText("ChatV2 user");
                        statusComboBox.getSelectionModel().select(UserStatus.ONLINE);
                    });
                }
            }).start();
        } else {
            // Set default values if not authenticated
            usernameLabel.setText("Username");
            fullNameField.setText("John Doe");
            bioTextArea.setText("ChatV2 user");
            statusComboBox.getSelectionModel().select(UserStatus.ONLINE);
        }
    }

    /**
     * Handles change avatar button click.
     */
    @FXML
    private void handleChangeAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Change Avatar");
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
                    showAlert("Error", "Avatar image must be less than 1MB");
                    return;
                }

                // Load image
                BufferedImage originalImage = ImageIO.read(file);
                if (originalImage == null) {
                    showAlert("Error", "Invalid image file");
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

                log.info("Avatar changed: {} (size: {} bytes)", file.getName(), avatarData.length);

            } catch (Exception e) {
                log.error("Failed to change avatar", e);
                showAlert("Error", "Failed to change avatar: " + e.getMessage());
            }
        }
    }

    /**
     * Handles save button click.
     */
    @FXML
    private void handleSave() {
        String fullName = fullNameField.getText().trim();
        String bio = bioTextArea.getText().trim();
        UserStatus status = statusComboBox.getSelectionModel().getSelectedItem();

        // Validate inputs
        if (!validateInputs(fullName, bio)) {
            return;
        }

        log.info("Saving profile: fullName={}, status={}", fullName, status);

        // Disable button
        saveButton.setDisable(true);
        saveButton.setText("Saving...");

        // Save in background
        new Thread(() -> {
            try {
                // Send profile update to server
                if (chatClient != null && chatClient.isAuthenticated()) {
                    chatClient.updateProfile(fullName, bio, avatarData).get();
                    log.info("Profile saved successfully");
                    Platform.runLater(() -> {
                        showAlert("Success", "Profile saved successfully");
                        saveButton.setDisable(false);
                        saveButton.setText("Save");
                    });
                } else {
                    // Simulate network delay if not authenticated
                    Thread.sleep(1000);
                    log.warn("Cannot save profile: not authenticated");
                    Platform.runLater(() -> {
                        showAlert("Warning", "Cannot save profile: not connected to server");
                        saveButton.setDisable(false);
                        saveButton.setText("Save");
                    });
                }

            } catch (Exception e) {
                log.error("Failed to save profile", e);
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to save profile: " + e.getMessage());
                    saveButton.setDisable(false);
                    saveButton.setText("Save");
                });
            }
        }).start();
    }

    /**
     * Handles back button click.
     */
    @FXML
    private void handleBack() {
        log.info("Navigating back to chat");
        ChatClientApp.getInstance().showChatScene();
    }

    /**
     * Validates profile inputs.
     */
    private boolean validateInputs(String fullName, String bio) {
        if (fullName.isEmpty()) {
            showAlert("Error", "Full name is required");
            return false;
        }

        if (fullName.length() < 2) {
            showAlert("Error", "Full name must be at least 2 characters");
            return false;
        }

        if (fullName.length() > 100) {
            showAlert("Error", "Full name must not exceed 100 characters");
            return false;
        }

        if (bio.length() > 500) {
            showAlert("Error", "Bio must not exceed 500 characters");
            return false;
        }

        return true;
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
}
