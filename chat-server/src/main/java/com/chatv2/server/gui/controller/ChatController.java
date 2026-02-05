package com.chatv2.server.gui.controller;

import com.chatv2.common.model.Chat;
import com.chatv2.common.model.ChatType;
import com.chatv2.common.model.UserProfile;
import com.chatv2.server.gui.ServerAdminApp;
import com.chatv2.server.manager.ChatManager;
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
import java.util.Set;
import java.util.UUID;

/**
 * Controller for the Chat Management view.
 * Provides viewing and managing chats and their participants.
 */
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private ServerAdminApp mainApp;
    private ChatManager chatManager;
    private UserManager userManager;

    private ObservableList<Chat> chats = FXCollections.observableArrayList();
    private ObservableList<UserProfile> participants = FXCollections.observableArrayList();

    @FXML private TableView<Chat> chatTable;
    @FXML private TableColumn<Chat, String> chatIdColumn;
    @FXML private TableColumn<Chat, String> chatNameColumn;
    @FXML private TableColumn<Chat, ChatType> chatTypeColumn;
    @FXML private TableColumn<Chat, String> ownerColumn;
    @FXML private TableColumn<Chat, Integer> participantsCountColumn;
    @FXML private TableColumn<Chat, String> createdAtColumn;

    @FXML private ListView<UserProfile> participantsListView;

    @FXML private Button refreshButton;
    @FXML private Button viewParticipantsButton;
    @FXML private Button viewHistoryButton;

    /**
     * Sets the main application instance.
     *
     * @param mainApp the ServerAdminApp instance
     */
    public void setMainApp(ServerAdminApp mainApp) {
        this.mainApp = mainApp;
        this.chatManager = mainApp.getDatabaseManager().getChatManager();
        this.userManager = mainApp.getDatabaseManager().getUserManager();
    }

    /**
     * Initializes the controller.
     * Sets up table columns and cell value factories.
     */
    @FXML
    private void initialize() {
        log.debug("Initializing ChatController");

        // Configure chat ID column
        chatIdColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().chatId().toString().substring(0, 8) + "..."
        ));

        // Configure chat name column
        chatNameColumn.setCellValueFactory(data -> {
            String name = data.getValue().name();
            return new SimpleStringProperty(
                name != null ? name : (data.getValue().chatType() == ChatType.PRIVATE ? "Private Chat" : "Unnamed Group")
            );
        });

        // Configure chat type column
        chatTypeColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().chatType()));
        chatTypeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(ChatType type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                } else {
                    setText(type.getDisplayName());
                }
            }
        });

        // Configure owner column
        ownerColumn.setCellValueFactory(data -> {
            UUID ownerId = data.getValue().ownerId();
            return new SimpleStringProperty(
                ownerId != null ? ownerId.toString().substring(0, 8) + "..." : "N/A"
            );
        });

        // Configure participants count column
        participantsCountColumn.setCellValueFactory(data ->
            new SimpleObjectProperty<>(data.getValue().participantCount())
        );

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

        // Set items to table and list
        chatTable.setItems(chats);
        participantsListView.setItems(participants);

        // Load initial chats
        loadChats();
    }

    /**
     * Loads all chats from the database.
     */
    private void loadChats() {
        Task<List<Chat>> loadTask = new Task<>() {
            @Override
            protected List<Chat> call() {
                try {
                    List<Chat> allChats = mainApp.getDatabaseManager().getChatRepository().findAll();
                    log.debug("Found {} chats in database", allChats.size());
                    return allChats;
                } catch (Exception e) {
                    log.error("Error loading chats", e);
                    throw new RuntimeException("Failed to load chats", e);
                }
            }

            @Override
            protected void succeeded() {
                chats.clear();
                chats.addAll(getValue());
                log.info("Loaded {} chats", chats.size());
            }

            @Override
            protected void failed() {
                Platform.runLater(() ->
                    mainApp.showErrorAlert("Load Error", "Failed to load chats", getException().getMessage())
                );
            }
        };

        new Thread(loadTask).start();
    }

    /**
     * Loads participants for the selected chat.
     */
    private void loadParticipants(UUID chatId) {
        Task<List<UserProfile>> loadTask = new Task<>() {
            @Override
            protected List<UserProfile> call() throws Exception {
                Set<UUID> participantIds = chatManager.getParticipants(chatId).get();
                log.debug("Found {} participants for chat {}", participantIds.size(), chatId);

                return participantIds.stream()
                    .map(userId -> {
                        try {
                            return userManager.getProfile(userId).get();
                        } catch (Exception e) {
                            log.warn("Failed to load profile for user {}", userId, e);
                            return null;
                        }
                    })
                    .filter(user -> user != null)
                    .toList();
            }

            @Override
            protected void succeeded() {
                participants.clear();
                participants.addAll(getValue());
                log.info("Loaded {} participants", participants.size());
            }

            @Override
            protected void failed() {
                Platform.runLater(() ->
                    mainApp.showErrorAlert("Load Error", "Failed to load participants", getException().getMessage())
                );
            }
        };

        new Thread(loadTask).start();
    }

    /**
     * Handles Refresh button click.
     */
    @FXML
    private void handleRefresh() {
        log.debug("Refresh button clicked");
        loadChats();
    }

    /**
     * Handles View Participants button click.
     */
    @FXML
    private void handleViewParticipants() {
        log.debug("View Participants button clicked");
        Chat selectedChat = chatTable.getSelectionModel().getSelectedItem();
        if (selectedChat == null) {
            mainApp.showErrorAlert("Selection Error", "No Chat Selected", "Please select a chat to view participants.");
            return;
        }
        loadParticipants(selectedChat.chatId());
    }

    /**
     * Handles View History button click.
     */
    @FXML
    private void handleViewHistory() {
        log.debug("View History button clicked");
        Chat selectedChat = chatTable.getSelectionModel().getSelectedItem();
        if (selectedChat == null) {
            mainApp.showErrorAlert("Selection Error", "No Chat Selected", "Please select a chat to view message history.");
            return;
        }
        showChatHistoryDialog(selectedChat);
    }

    /**
     * Shows a dialog with chat information and message history.
     *
     * @param chat the chat to display
     */
    private void showChatHistoryDialog(Chat chat) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chat Details");
        dialog.setHeaderText("Chat: " + (chat.name() != null ? chat.name() : (chat.chatType() == ChatType.PRIVATE ? "Private Chat" : "Group")));

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        // Create content grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        // Add chat information
        grid.add(new javafx.scene.control.Label("Chat ID:"), 0, 0);
        grid.add(new javafx.scene.control.Label(chat.chatId().toString()), 1, 0);

        grid.add(new javafx.scene.control.Label("Type:"), 0, 1);
        grid.add(new javafx.scene.control.Label(chat.chatType().getDisplayName()), 1, 1);

        grid.add(new javafx.scene.control.Label("Owner:"), 0, 2);
        grid.add(new javafx.scene.control.Label(chat.ownerId().toString()), 1, 2);

        grid.add(new javafx.scene.control.Label("Participants:"), 0, 3);
        grid.add(new javafx.scene.control.Label(String.valueOf(chat.participantCount())), 1, 3);

        if (chat.name() != null) {
            grid.add(new javafx.scene.control.Label("Name:"), 0, 4);
            grid.add(new javafx.scene.control.Label(chat.name()), 1, 4);
        }

        if (chat.description() != null) {
            grid.add(new javafx.scene.control.Label("Description:"), 0, 5);
            javafx.scene.control.Label descLabel = new javafx.scene.control.Label(chat.description());
            descLabel.setWrapText(true);
            grid.add(descLabel, 1, 5);
        }

        // Add message history placeholder
        TextArea messageHistory = new TextArea();
        messageHistory.setEditable(false);
        messageHistory.setPrefRowCount(15);
        messageHistory.setWrapText(true);
        messageHistory.setText("Message history loading...\n\n(Note: Full message history requires MessageManager integration)");

        grid.add(new javafx.scene.control.Label("Message History:"), 0, 6);
        grid.add(messageHistory, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait();
    }
}
