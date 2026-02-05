package com.chatv2.server.gui.controller;

import com.chatv2.common.model.UserProfile;
import com.chatv2.common.model.UserStatus;
import com.chatv2.server.gui.ServerAdminApp;
import com.chatv2.server.manager.UserManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for the User Management view.
 * Provides CRUD operations for user management.
 */
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private ServerAdminApp mainApp;
    private UserManager userManager;
    private ObservableList<UserProfile> users = FXCollections.observableArrayList();

    @FXML private TableView<UserProfile> userTable;
    @FXML private TableColumn<UserProfile, String> usernameColumn;
    @FXML private TableColumn<UserProfile, String> fullNameColumn;
    @FXML private TableColumn<UserProfile, UserStatus> statusColumn;
    @FXML private TableColumn<UserProfile, String> createdAtColumn;
    @FXML private TextField searchField;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;

    /**
     * Sets the main application instance.
     *
     * @param mainApp the ServerAdminApp instance
     */
    public void setMainApp(ServerAdminApp mainApp) {
        this.mainApp = mainApp;
        this.userManager = mainApp.getDatabaseManager().getUserManager();
    }

    /**
     * Initializes the controller.
     * Sets up table columns and cell value factories.
     */
    @FXML
    private void initialize() {
        log.debug("Initializing UserController");

        // Configure username column
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().username()
        ));

        // Configure full name column
        fullNameColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().fullName() != null ? data.getValue().fullName() : "N/A"
        ));

        // Configure status column with colored cell
        statusColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().status()));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(UserStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status.getDisplayName());
                    String color = status.getColor();
                    setStyle("-fx-text-fill: " + color + ";");
                }
            }
        });

        // Configure created at column
        createdAtColumn.setCellValueFactory(data -> {
            Instant createdAt = data.getValue().createdAt();
            if (createdAt != null) {
                return new SimpleStringProperty(
                    DateTimeFormatter.ISO_INSTANT.format(createdAt)
                );
            }
            return new SimpleStringProperty("N/A");
        });

        // Set items to table
        userTable.setItems(users);

        // Load initial users
        loadUsers();

        // Setup search listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> handleSearch());
    }

    /**
     * Loads all users from the database.
     */
    private void loadUsers() {
        Task<List<UserProfile>> loadTask = new Task<>() {
            @Override
            protected List<UserProfile> call() {
                try {
                    List<UUID> allUserIds = mainApp.getDatabaseManager().getUserRepository().findAll()
                        .stream()
                        .map(UserProfile::userId)
                        .toList();

                    return mainApp.getDatabaseManager().getUserRepository().findAll();
                } catch (Exception e) {
                    log.error("Error loading users", e);
                    throw new RuntimeException("Failed to load users", e);
                }
            }

            @Override
            protected void succeeded() {
                users.clear();
                users.addAll(getValue());
                log.info("Loaded {} users", users.size());
            }

            @Override
            protected void failed() {
                Platform.runLater(() ->
                    mainApp.showErrorAlert("Load Error", "Failed to load users", getException().getMessage())
                );
            }
        };

        new Thread(loadTask).start();
    }

    /**
     * Handles search functionality.
     */
    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            loadUsers();
            return;
        }

        Task<List<UserProfile>> searchTask = new Task<>() {
            @Override
            protected List<UserProfile> call() throws Exception {
                return userManager.searchUsers(query, 100).get();
            }

            @Override
            protected void succeeded() {
                users.clear();
                users.addAll(getValue());
                log.info("Found {} users matching query: {}", users.size(), query);
            }

            @Override
            protected void failed() {
                Platform.runLater(() ->
                    mainApp.showErrorAlert("Search Error", "Failed to search users", getException().getMessage())
                );
            }
        };

        new Thread(searchTask).start();
    }

    /**
     * Handles Add User button click.
     */
    @FXML
    private void handleAddUser() {
        log.debug("Add User button clicked");
        showUserDialog(null);
    }

    /**
     * Handles Edit User button click.
     */
    @FXML
    private void handleEditUser() {
        log.debug("Edit User button clicked");
        UserProfile selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            mainApp.showErrorAlert("Selection Error", "No User Selected", "Please select a user to edit.");
            return;
        }
        showUserDialog(selectedUser);
    }

    /**
     * Handles Delete User button click.
     */
    @FXML
    private void handleDeleteUser() {
        log.debug("Delete User button clicked");
        UserProfile selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            mainApp.showErrorAlert("Selection Error", "No User Selected", "Please select a user to delete.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete User");
        confirmAlert.setHeaderText("Delete user: " + selectedUser.username() + "?");
        confirmAlert.setContentText("This action cannot be undone. All user data will be lost.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Void> deleteTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    mainApp.getDatabaseManager().getUserRepository().delete(selectedUser.userId());
                    return null;
                }

                @Override
                protected void succeeded() {
                    mainApp.showInfoAlert("Success", "User Deleted", "User " + selectedUser.username() + " has been deleted.");
                    loadUsers();
                }

                @Override
                protected void failed() {
                    Platform.runLater(() ->
                        mainApp.showErrorAlert("Delete Error", "Failed to delete user", getException().getMessage())
                    );
                }
            };

            new Thread(deleteTask).start();
        }
    }

    /**
     * Handles Refresh button click.
     */
    @FXML
    private void handleRefresh() {
        log.debug("Refresh button clicked");
        loadUsers();
    }

    /**
     * Shows a dialog for creating or editing a user.
     *
     * @param user the user to edit, or null for creating a new user
     */
    private void showUserDialog(UserProfile user) {
        Dialog<UserProfile> dialog = new Dialog<>();
        dialog.setTitle(user == null ? "Add User" : "Edit User");
        dialog.setHeaderText(user == null ? "Create a new user" : "Edit user: " + user.username());

        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full Name");
        TextField bioField = new TextField();
        bioField.setPromptText("Bio");

        if (user != null) {
            usernameField.setText(user.username());
            usernameField.setEditable(false);
            fullNameField.setText(user.fullName());
            bioField.setText(user.bio());
            passwordField.setPromptText("Password (leave empty to keep current)");
            passwordField.setEditable(false);
        }

        grid.add(new javafx.scene.control.Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new javafx.scene.control.Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new javafx.scene.control.Label("Full Name:"), 0, 2);
        grid.add(fullNameField, 1, 2);
        grid.add(new javafx.scene.control.Label("Bio:"), 0, 3);
        grid.add(bioField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Enable/Disable save button based on validation
        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        usernameField.textProperty().addListener((observable, oldValue, newValue) ->
            saveButton.setDisable(newValue.trim().isEmpty()));

        if (user == null) {
            passwordField.textProperty().addListener((observable, oldValue, newValue) ->
                saveButton.setDisable(newValue.trim().isEmpty() || usernameField.getText().trim().isEmpty()));
        }

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String username = usernameField.getText().trim();
                String password = passwordField.getText();
                String fullName = fullNameField.getText().trim();
                String bio = bioField.getText().trim();

                if (user == null) {
                    // Create new user
                    try {
                        return userManager.register(username, password, fullName, bio).get();
                    } catch (Exception e) {
                        log.error("Failed to create user", e);
                        Platform.runLater(() ->
                            mainApp.showErrorAlert("Create Error", "Failed to create user", e.getMessage())
                        );
                    }
                } else {
                    // Update existing user
                    try {
                        return userManager.updateProfile(user.userId(), fullName, bio, null).get();
                    } catch (Exception e) {
                        log.error("Failed to update user", e);
                        Platform.runLater(() ->
                            mainApp.showErrorAlert("Update Error", "Failed to update user", e.getMessage())
                        );
                    }
                }
            }
            return null;
        });

        Optional<UserProfile> result = dialog.showAndWait();
        result.ifPresent(savedUser -> {
            mainApp.showInfoAlert("Success", user == null ? "User Created" : "User Updated",
                "User " + savedUser.username() + " has been saved.");
            loadUsers();
        });
    }
}
