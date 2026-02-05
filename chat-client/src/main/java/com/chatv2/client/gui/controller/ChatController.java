package com.chatv2.client.gui.controller;

import com.chatv2.client.core.ChatClient;
import com.chatv2.client.gui.ChatClientApp;
import com.chatv2.client.gui.model.ChatListItem;
import com.chatv2.client.gui.model.ParticipantListItem;
import com.chatv2.common.model.Message;
import com.chatv2.common.model.MessageType;
import com.chatv2.common.model.UserProfile;
import com.chatv2.common.model.UserStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Controller for main chat screen.
 * Handles message display, chat list, and participants.
 */
public class ChatController implements Initializable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatController.class);

    @FXML private ListView<ChatListItem> chatListView;
    @FXML private ScrollPane messageScrollPane;
    @FXML private VBox messageVBox;
    @FXML private ListView<ParticipantListItem> participantsListView;
    @FXML private TextArea messageTextArea;
    @FXML private Button sendButton;
    @FXML private Button attachButton;
    @FXML private Label currentChatLabel;
    @FXML private Button logoutButton;
    @FXML private Button profileButton;

    private ChatClient chatClient;
    private ObservableList<ChatListItem> chatList;
    private ObservableList<ParticipantListItem> participantList;
    private UUID currentChatId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing ChatController");

        // Get chat client
        ChatClientApp app = ChatClientApp.getInstance();
        if (app != null) {
            chatClient = app.getChatClient();
        }

        // Initialize lists
        chatList = FXCollections.observableArrayList();
        participantList = FXCollections.observableArrayList();

        if (chatListView != null) {
            chatListView.setItems(chatList);
        }
        if (participantsListView != null) {
            participantsListView.setItems(participantList);
        }

        // Custom list cell factories
        chatListView.setCellFactory(lv -> new ChatListCell());
        participantsListView.setCellFactory(lv -> new ParticipantListCell());

        // Setup button actions
        sendButton.setOnAction(e -> handleSendMessage());
        attachButton.setOnAction(e -> handleAttachFile());
        logoutButton.setOnAction(e -> handleLogout());
        profileButton.setOnAction(e -> handleProfile());

        // Setup chat selection listener
        chatListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> handleChatSelection(newVal));

        // Setup message input handling
        messageTextArea.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                if (e.isShiftDown()) {
                    // Shift+Enter for new line
                    messageTextArea.appendText("\n");
                } else {
                    // Enter to send
                    e.consume();
                    handleSendMessage();
                }
            }
        });

        // Subscribe to incoming messages
        if (chatClient != null) {
            chatClient.registerMessageConsumer(this::handleIncomingMessage);
        }

        // Load initial data
        loadChatList();

        log.info("ChatController initialized");
    }

    /**
     * Loads the list of chats.
     */
    private void loadChatList() {
        log.debug("Loading chat list");

        // Mock data for demonstration
        chatList.add(new ChatListItem(
            UUID.randomUUID(),
            "General Chat",
            "Welcome to the chat!",
            java.time.Instant.now(),
            0,
            null
        ));

        chatList.add(new ChatListItem(
            UUID.randomUUID(),
            "Tech Discussion",
            "Anyone know JavaFX?",
            java.time.Instant.now().minusSeconds(3600),
            3,
            null
        ));

        chatListView.getSelectionModel().selectFirst();
    }

    /**
     * Handles chat selection.
     */
    private void handleChatSelection(ChatListItem chat) {
        if (chat == null) {
            return;
        }

        currentChatId = chat.chatId();
        currentChatLabel.setText(chat.name());

        log.debug("Selected chat: {}", chat.name());

        // Clear messages
        messageVBox.getChildren().clear();

        // Load messages
        loadMessages();

        // Load participants
        loadParticipants();
    }

    /**
     * Loads messages for current chat.
     */
    private void loadMessages() {
        log.debug("Loading messages for chat: {}", currentChatId);

        // Mock data for demonstration
        addMessage("System", "Welcome to the chat!", java.time.Instant.now(), true);
        addMessage("User1", "Hello everyone!", java.time.Instant.now().minusSeconds(60), false);
        addMessage("User2", "Hi there!", java.time.Instant.now().minusSeconds(30), false);
    }

    /**
     * Loads participants for current chat.
     */
    private void loadParticipants() {
        log.debug("Loading participants for chat: {}", currentChatId);

        participantList.clear();

        // Mock data for demonstration
        participantList.add(new ParticipantListItem(
            UUID.randomUUID(),
            "User1",
            UserStatus.ONLINE,
            null
        ));

        participantList.add(new ParticipantListItem(
            UUID.randomUUID(),
            "User2",
            UserStatus.OFFLINE,
            null
        ));
    }

    /**
     * Handles send message button click.
     */
    @FXML
    private void handleSendMessage() {
        String content = messageTextArea.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        log.debug("Sending message: {}", content);

        // Add message locally (optimistic update)
        addMessage(chatClient.getCurrentUserId() != null ?
                   chatClient.getCurrentUserId().toString() : "Me",
                   content, java.time.Instant.now(), true);

        // Send to server
        Message message = Message.createNew(
            currentChatId,
            chatClient.getCurrentUserId() != null ? chatClient.getCurrentUserId() : UUID.randomUUID(),
            content,
            MessageType.TEXT
        );

        if (chatClient != null) {
            chatClient.sendMessage(message).thenRun(() -> {
                log.debug("Message sent successfully");
            }).exceptionally(ex -> {
                log.error("Failed to send message", ex);
                Platform.runLater(() -> showAlert("Send Failed", "Failed to send message"));
                return null;
            });
        }

        // Clear input
        messageTextArea.clear();
    }

    /**
     * Handles attach file button click.
     */
    @FXML
    private void handleAttachFile() {
        log.info("Attach file feature not implemented yet");
        showAlert("Not Implemented", "File attachment feature is not yet implemented");
    }

    /**
     * Handles logout button click.
     */
    @FXML
    private void handleLogout() {
        log.info("Logging out");

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Logout");
        alert.setHeaderText("Do you want to logout?");
        alert.setContentText("You will return to the login screen.");

        alert.showAndWait().ifPresent(response -> {
            if (response.getButtonData().isDefaultButton()) {
                if (chatClient != null) {
                    chatClient.disconnect().thenRun(() -> {
                        Platform.runLater(() -> ChatClientApp.getInstance().showLoginScene());
                    });
                } else {
                    ChatClientApp.getInstance().showLoginScene();
                }
            }
        });
    }

    /**
     * Handles profile button click.
     */
    @FXML
    private void handleProfile() {
        log.info("Opening profile");
        ChatClientApp.getInstance().showProfileScene();
    }

    /**
     * Handles incoming message.
     */
    private void handleIncomingMessage(Message message) {
        Platform.runLater(() -> {
            log.debug("Received message: {}", message.messageId());
            addMessage(message.senderId().toString(),
                      message.content(),
                      message.createdAt(),
                      message.senderId().equals(chatClient.getCurrentUserId()));
        });
    }

    /**
     * Adds a message bubble to the chat.
     */
    private void addMessage(String sender, String content, java.time.Instant timestamp, boolean isOwn) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        HBox messageContainer = new HBox(5);
        messageContainer.getStyleClass().add("message-container");

        if (isOwn) {
            messageContainer.getStyleClass().add("own-message");
            messageContainer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        } else {
            messageContainer.getStyleClass().add("other-message");
            messageContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }

        VBox messageBubble = new VBox(3);
        messageBubble.getStyleClass().add("message-bubble");

        Label senderLabel = new Label(sender);
        senderLabel.getStyleClass().add("message-sender");

        Label contentLabel = new Label(content);
        contentLabel.getStyleClass().add("message-content");
        contentLabel.setWrapText(true);

        Label timeLabel = new Label(timestamp.atZone(java.time.ZoneId.systemDefault()).format(formatter));
        timeLabel.getStyleClass().add("message-time");

        messageBubble.getChildren().addAll(senderLabel, contentLabel, timeLabel);
        messageContainer.getChildren().add(messageBubble);

        Platform.runLater(() -> {
            messageVBox.getChildren().add(messageContainer);
            messageScrollPane.setVvalue(1.0); // Auto-scroll to bottom
        });
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

    /**
     * Custom list cell for chat items.
     */
    private static class ChatListCell extends ListCell<ChatListItem> {
        private final VBox content = new VBox(3);
        private final Label nameLabel = new Label();
        private final Label lastMessageLabel = new Label();
        private final HBox footerBox = new HBox(10);
        private final Label timeLabel = new Label();
        private final Label unreadLabel = new Label();

        public ChatListCell() {
            super();
            content.setPadding(new javafx.geometry.Insets(8, 8, 8, 8));

            nameLabel.getStyleClass().add("chat-name");
            lastMessageLabel.getStyleClass().add("chat-last-message");
            timeLabel.getStyleClass().add("chat-time");
            unreadLabel.getStyleClass().add("chat-unread");

            footerBox.getChildren().addAll(timeLabel, unreadLabel);
            content.getChildren().addAll(nameLabel, lastMessageLabel, footerBox);
        }

        @Override
        protected void updateItem(ChatListItem chat, boolean empty) {
            super.updateItem(chat, empty);

            if (empty || chat == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(chat.name());
                lastMessageLabel.setText(chat.getDisplayLastMessage());
                timeLabel.setText(chat.getFormattedTime());

                if (chat.unreadCount() > 0) {
                    unreadLabel.setText(String.valueOf(chat.unreadCount()));
                    unreadLabel.setVisible(true);
                } else {
                    unreadLabel.setVisible(false);
                }

                setGraphic(content);
            }
        }
    }

    /**
     * Custom list cell for participant items.
     */
    private static class ParticipantListCell extends ListCell<ParticipantListItem> {
        private final HBox content = new HBox(10);
        private final VBox textContainer = new VBox(2);
        private final Label usernameLabel = new Label();
        private final Label statusLabel = new Label();
        private final Circle statusIndicator = new Circle(5);

        public ParticipantListCell() {
            super();
            content.setPadding(new javafx.geometry.Insets(8, 8, 8, 8));
            content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            usernameLabel.getStyleClass().add("participant-username");
            statusLabel.getStyleClass().add("participant-status");

            textContainer.getChildren().addAll(usernameLabel, statusLabel);
            content.getChildren().addAll(textContainer, statusIndicator);
        }

        @Override
        protected void updateItem(ParticipantListItem participant, boolean empty) {
            super.updateItem(participant, empty);

            if (empty || participant == null) {
                setGraphic(null);
            } else {
                usernameLabel.setText(participant.username());
                statusLabel.setText(participant.getStatusDisplay());

                // Status indicator color
                switch (participant.status()) {
                    case ONLINE -> statusIndicator.setStyle("-fx-fill: #4CAF50;");
                    case AWAY -> statusIndicator.setStyle("-fx-fill: #FFC107;");
                    case OFFLINE -> statusIndicator.setStyle("-fx-fill: #9E9E9E;");
                }

                setGraphic(content);
            }
        }
    }
}
