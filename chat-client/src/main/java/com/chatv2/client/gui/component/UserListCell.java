package com.chatv2.client.gui.component;

import com.chatv2.client.gui.model.UserListItem;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Custom ListCell for displaying users in ListView.
 * Uses AvatarImageView and StatusIndicator components.
 */
public class UserListCell extends ListCell<UserListItem> {

    private static final Logger log = LogManager.getLogger(UserListCell.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private HBox container;
    private AvatarImageView avatarImageView;
    private StackPane avatarWrapper;
    private StatusIndicator statusIndicator;
    private VBox userInfo;
    private javafx.scene.control.Label usernameLabel;
    private javafx.scene.control.Label statusLabel;
    private javafx.scene.control.Label lastActivityLabel;

    /**
     * Default constructor.
     */
    public UserListCell() {
        initialize();
    }

    /**
     * Initialize the cell layout.
     */
    private void initialize() {
        // Create container
        container = new HBox(10);
        container.getStyleClass().add("user-list-cell");
        container.setMaxWidth(Double.MAX_VALUE);

        // Create avatar wrapper for stacking avatar and status indicator
        avatarWrapper = new StackPane();
        avatarWrapper.setPrefSize(40, 40);

        // Create avatar image view
        avatarImageView = new AvatarImageView(AvatarImageView.AvatarSize.SMALL);
        avatarImageView.setMouseTransparent(true);
        avatarWrapper.getChildren().add(avatarImageView);

        // Create status indicator (positioned at bottom-right of avatar)
        statusIndicator = new StatusIndicator(StatusIndicator.StatusSize.SMALL);
        StackPane.setAlignment(statusIndicator, javafx.geometry.Pos.BOTTOM_RIGHT);
        StackPane.setMargin(statusIndicator, new javafx.geometry.Insets(-3, -3, 0, 0));
        avatarWrapper.getChildren().add(statusIndicator);

        // Create user info container
        userInfo = new VBox(3);
        userInfo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Create username label
        usernameLabel = new javafx.scene.control.Label();
        usernameLabel.getStyleClass().add("participant-username");
        usernameLabel.setWrapText(false);
        usernameLabel.setMaxWidth(200);

        // Create status label
        statusLabel = new javafx.scene.control.Label();
        statusLabel.getStyleClass().add("participant-status");
        statusLabel.setWrapText(false);
        statusLabel.setMaxWidth(200);

        // Create last activity label (optional)
        lastActivityLabel = new javafx.scene.control.Label();
        lastActivityLabel.getStyleClass().add("participant-status");
        lastActivityLabel.setVisible(false);

        // Add labels to info container
        userInfo.getChildren().add(usernameLabel);
        userInfo.getChildren().add(statusLabel);
        userInfo.getChildren().add(lastActivityLabel);

        // Add components to container
        HBox.setHgrow(userInfo, Priority.ALWAYS);
        container.getChildren().add(avatarWrapper);
        container.getChildren().add(userInfo);

        // Set graphic to container
        setGraphic(container);
    }

    @Override
    protected void updateItem(UserListItem item, boolean empty) {
        super.updateItem(item, empty);

        try {
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                clearTooltip();
            } else {
                setText(null);
                setGraphic(container);
                updateUserDisplay(item);
                updateTooltip(item);
            }
        } catch (Exception e) {
            log.error("Error updating UserListCell item", e);
            setText("Error displaying user");
            setGraphic(null);
        }
    }

    /**
     * Update the display of the user item.
     *
     * @param item the user item to display
     */
    private void updateUserDisplay(UserListItem item) {
        // Update avatar
        if (item.hasAvatar()) {
            avatarImageView.setAvatar(item.avatar());
        } else {
            avatarImageView.setAvatar(null);
            avatarImageView.setUsername(item.username());
        }

        // Update status indicator
        statusIndicator.setStatus(item.status());

        // Update username
        usernameLabel.setText(item.username());

        // Update status label
        statusLabel.setText(item.status().getDisplayName());
        statusLabel.setStyle("-fx-text-fill: " + item.status().getColor() + ";");

        // Update last activity (optional - only show if not ONLINE)
        if (item.status() != com.chatv2.common.model.UserStatus.ONLINE && 
            item.lastActivity() != null && 
            item.lastActivity().toEpochMilli() > 0) {
            try {
                String lastActivityTime = item.lastActivity()
                    .atZone(ZoneId.systemDefault())
                    .format(TIME_FORMATTER);
                lastActivityLabel.setText("Last seen: " + lastActivityTime);
                lastActivityLabel.setVisible(true);
            } catch (Exception e) {
                lastActivityLabel.setVisible(false);
            }
        } else {
            lastActivityLabel.setVisible(false);
        }
    }

    /**
     * Update the tooltip with user information.
     *
     * @param item the user item
     */
    private void updateTooltip(UserListItem item) {
        StringBuilder tooltipText = new StringBuilder();
        tooltipText.append("Username: ").append(item.username()).append("\n");
        tooltipText.append("Status: ").append(item.status().getDisplayName()).append("\n");

        if (item.lastActivity() != null && item.lastActivity().toEpochMilli() > 0) {
            try {
                String lastActivityTime = item.lastActivity()
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                tooltipText.append("Last Activity: ").append(lastActivityTime);
            } catch (Exception e) {
                // Ignore formatting errors
            }
        }

        Tooltip tooltip = new Tooltip(tooltipText.toString());
        setTooltip(tooltip);
    }

    /**
     * Clear the tooltip.
     */
    private void clearTooltip() {
        setTooltip(null);
    }

    /**
     * Get the avatar image view used in this cell.
     *
     * @return the AvatarImageView instance
     */
    public AvatarImageView getAvatarImageView() {
        return avatarImageView;
    }

    /**
     * Get the status indicator used in this cell.
     *
     * @return the StatusIndicator instance
     */
    public StatusIndicator getStatusIndicator() {
        return statusIndicator;
    }

    /**
     * Refresh the display of the current item.
     */
    public void refreshDisplay() {
        UserListItem item = getItem();
        if (item != null) {
            updateUserDisplay(item);
            updateTooltip(item);
        }
    }
}
