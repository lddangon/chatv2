package com.chatv2.client.gui.component;

import com.chatv2.common.model.Message;
import com.chatv2.common.model.MessageType;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Custom Control for displaying chat messages.
 * Supports different styles for own and other messages.
 */
public class MessageBubble extends HBox {

    private static final Logger log = LogManager.getLogger(MessageBubble.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // CSS pseudo classes
    private static final PseudoClass OWN_PSEUDO_CLASS = PseudoClass.getPseudoClass("own");
    private static final PseudoClass OTHER_PSEUDO_CLASS = PseudoClass.getPseudoClass("other");
    private static final PseudoClass EDITED_PSEUDO_CLASS = PseudoClass.getPseudoClass("edited");
    private static final PseudoClass READ_PSEUDO_CLASS = PseudoClass.getPseudoClass("read");
    private static final PseudoClass UNREAD_PSEUDO_CLASS = PseudoClass.getPseudoClass("unread");

    // Configuration
    private static final double MAX_WIDTH_RATIO = 0.7;
    private static final double PADDING = 10.0;
    private static final double SPACING = 5.0;

    // UI Components
    private AvatarImageView avatarImageView;
    private VBox bubbleContainer;
    private Label usernameLabel;
    private TextFlow contentFlow;
    private Label timeLabel;
    private Label statusLabel;

    // State
    private Message message;
    private boolean isOwnMessage;
    private boolean isRead = false;
    private boolean isEdited = false;
    private UUID currentUserId;
    private String senderUsername;

    /**
     * Constructor with message and ownership flag.
     *
     * @param message the message to display
     * @param isOwnMessage true if this is the current user's message
     */
    public MessageBubble(Message message, boolean isOwnMessage) {
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }
        this.message = message;
        this.isOwnMessage = isOwnMessage;
        this.isEdited = message.isEdited();
        initialize();
    }

    /**
     * Constructor with message, ownership, and username.
     *
     * @param message the message to display
     * @param isOwnMessage true if this is the current user's message
     * @param senderUsername the username of the sender
     */
    public MessageBubble(Message message, boolean isOwnMessage, String senderUsername) {
        this(message, isOwnMessage);
        this.senderUsername = senderUsername;
        updateDisplay();
    }

    /**
     * Initialize the component layout.
     */
    private void initialize() {
        getStyleClass().add("message-bubble");
        setSpacing(SPACING);

        // Create avatar view (optional, shown for other messages)
        avatarImageView = new AvatarImageView(AvatarImageView.AvatarSize.SMALL);
        avatarImageView.setVisible(!isOwnMessage);
        avatarImageView.setManaged(!isOwnMessage);

        // Create bubble container
        bubbleContainer = new VBox(SPACING);
        bubbleContainer.getStyleClass().add("bubble-container");
        bubbleContainer.setPadding(new Insets(PADDING));
        bubbleContainer.setMaxWidth(Double.MAX_VALUE);

        // Create username label (for other messages in group chat)
        usernameLabel = new Label();
        usernameLabel.getStyleClass().add("message-sender");
        usernameLabel.setVisible(!isOwnMessage);
        usernameLabel.setManaged(!isOwnMessage);

        // Create content flow for formatted text
        contentFlow = new TextFlow();
        contentFlow.getStyleClass().add("message-content");
        contentFlow.setMaxWidth(Double.MAX_VALUE);

        // Create time label
        timeLabel = new Label();
        timeLabel.getStyleClass().add("message-time");

        // Create status label (read receipts, edited indicator)
        statusLabel = new Label();
        statusLabel.getStyleClass().add("message-status");

        // Add components to bubble container
        bubbleContainer.getChildren().add(usernameLabel);
        bubbleContainer.getChildren().add(contentFlow);
        bubbleContainer.getChildren().add(createMetadataRow());

        // Add components to main container
        if (isOwnMessage) {
            // Own message: bubble on right
            getChildren().add(bubbleContainer);
            setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        } else {
            // Other message: avatar + bubble on left
            getChildren().add(avatarImageView);
            getChildren().add(bubbleContainer);
            setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }

        // Set max width
        maxWidthProperty().bind(widthProperty().multiply(MAX_WIDTH_RATIO));

        // Update pseudo classes
        updatePseudoClasses();

        // Update display
        updateDisplay();
    }

    /**
     * Create the metadata row containing time and status.
     *
     * @return HBox with time and status
     */
    private HBox createMetadataRow() {
        HBox metadataRow = new HBox(SPACING);
        metadataRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        metadataRow.getChildren().add(timeLabel);
        metadataRow.getChildren().add(statusLabel);

        return metadataRow;
    }

    /**
     * Update the display based on current message and state.
     */
    private void updateDisplay() {
        try {
            // Update avatar if set
            if (!isOwnMessage && senderUsername != null) {
                avatarImageView.setUsername(senderUsername);
            }

            // Update username label
            if (!isOwnMessage && senderUsername != null) {
                usernameLabel.setText(senderUsername);
            }

            // Update content based on message type
            updateContent();

            // Update time label
            updateTimeLabel();

            // Update status label
            updateStatusLabel();

            // Update edited state
            updateEditedDisplay();

        } catch (Exception e) {
            log.error("Error updating MessageBubble display", e);
        }
    }

    /**
     * Update content based on message type.
     */
    private void updateContent() {
        contentFlow.getChildren().clear();

        MessageType messageType = message.messageType();

        if (messageType == MessageType.TEXT) {
            // Text message
            Text textNode = new Text(message.content());
            textNode.getStyleClass().add("message-text");
            contentFlow.getChildren().add(textNode);
        } else if (messageType == MessageType.IMAGE) {
            // Image message - display as base64 decoded image
            try {
                String base64Data = message.content();
                if (base64Data != null && !base64Data.isEmpty()) {
                    Image image = new Image("data:image/png;base64," + base64Data);
                    ImageView imageView = new ImageView(image);
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(200);
                    contentFlow.getChildren().add(imageView);
                }
            } catch (Exception e) {
                log.warn("Failed to load image for message {}", message.messageId(), e);
                Text errorText = new Text("[Image failed to load]");
                errorText.getStyleClass().add("message-error");
                contentFlow.getChildren().add(errorText);
            }
        } else if (messageType == MessageType.FILE) {
            // File attachment message
            Text fileText = new Text("📎 " + message.content());
            fileText.getStyleClass().add("message-file");
            contentFlow.getChildren().add(fileText);
        } else if (messageType == MessageType.VOICE) {
            // Voice message
            Text voiceText = new Text("🎤 Voice message");
            voiceText.getStyleClass().add("message-voice");
            contentFlow.getChildren().add(voiceText);
        } else if (messageType == MessageType.SYSTEM) {
            // System message
            Text systemText = new Text(message.content());
            systemText.getStyleClass().add("message-system");
            contentFlow.getChildren().add(systemText);
        }
    }

    /**
     * Update the time label.
     */
    private void updateTimeLabel() {
        try {
            String timeText = message.createdAt()
                .atZone(ZoneId.systemDefault())
                .format(TIME_FORMATTER);
            timeLabel.setText(timeText);
        } catch (Exception e) {
            timeLabel.setText("");
        }
    }

    /**
     * Update the status label with read receipts and edited indicator.
     */
    private void updateStatusLabel() {
        StringBuilder statusText = new StringBuilder();

        // Add edited indicator
        if (isEdited) {
            statusText.append("(edited) ");
        }

        // Add read receipts for own messages
        if (isOwnMessage) {
            int readCount = message.readBy() != null ? message.readBy().size() : 0;
            if (readCount > 0) {
                // Display checkmarks
                statusText.append(readCount == 1 ? "✓ " : "✓✓ ");
            }
        }

        statusLabel.setText(statusText.toString());
    }

    /**
     * Update the edited display state.
     */
    private void updateEditedDisplay() {
        pseudoClassStateChanged(EDITED_PSEUDO_CLASS, isEdited);
    }

    /**
     * Update CSS pseudo classes.
     */
    private void updatePseudoClasses() {
        pseudoClassStateChanged(OWN_PSEUDO_CLASS, isOwnMessage);
        pseudoClassStateChanged(OTHER_PSEUDO_CLASS, !isOwnMessage);
        updateReadPseudoClass();
    }

    /**
     * Update read pseudo class.
     */
    private void updateReadPseudoClass() {
        pseudoClassStateChanged(READ_PSEUDO_CLASS, isRead && isOwnMessage);
        pseudoClassStateChanged(UNREAD_PSEUDO_CLASS, !isRead && isOwnMessage);
    }

    /**
     * Set the read status of the message.
     *
     * @param read true if message is read
     */
    public void setReadStatus(boolean read) {
        if (this.isRead != read) {
            this.isRead = read;
            updateReadPseudoClass();
            updateStatusLabel();
        }
    }

    /**
     * Set the edited status of the message.
     *
     * @param edited true if message is edited
     */
    public void setEdited(boolean edited) {
        if (this.isEdited != edited) {
            this.isEdited = edited;
            updateEditedDisplay();
            updateStatusLabel();
        }
    }

    /**
     * Get the current read status.
     *
     * @return true if message is read
     */
    public boolean isRead() {
        return isRead;
    }

    /**
     * Get the current edited status.
     *
     * @return true if message is edited
     */
    public boolean isEdited() {
        return isEdited;
    }

    /**
     * Get the message.
     *
     * @return the message
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Check if this is the current user's message.
     *
     * @return true if own message
     */
    public boolean isOwnMessage() {
        return isOwnMessage;
    }

    /**
     * Set the avatar data for the sender.
     *
     * @param avatarData the avatar image data
     */
    public void setAvatar(byte[] avatarData) {
        if (!isOwnMessage) {
            avatarImageView.setAvatar(avatarData);
        }
    }

    /**
     * Set the current user ID for read status tracking.
     *
     * @param userId the current user ID
     */
    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
    }

    /**
     * Update the message with new data.
     *
     * @param updatedMessage the updated message
     */
    public void updateMessage(Message updatedMessage) {
        if (updatedMessage == null) {
            throw new IllegalArgumentException("updatedMessage cannot be null");
        }
        this.message = updatedMessage;
        this.isEdited = updatedMessage.isEdited();
        updateDisplay();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageBubble that = (MessageBubble) o;
        return isOwnMessage == that.isOwnMessage &&
            Objects.equals(message.messageId(), that.message.messageId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(message.messageId(), isOwnMessage);
    }

    /**
     * Create a message bubble for an own message.
     *
     * @param message the message
     * @return MessageBubble instance
     */
    public static MessageBubble createOwn(Message message) {
        return new MessageBubble(message, true);
    }

    /**
     * Create a message bubble for another user's message.
     *
     * @param message the message
     * @param username the sender's username
     * @return MessageBubble instance
     */
    public static MessageBubble createOther(Message message, String username) {
        return new MessageBubble(message, false, username);
    }
}
