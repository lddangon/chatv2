package com.chatv2.client.gui.component;

import com.chatv2.client.gui.model.UserListItem;
import com.chatv2.common.model.UserStatus;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for UserListCell component.
 * Tests the display of user information, status indicators, and avatar.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(ApplicationExtension.class)
class UserListCellTest extends JavaFXTestBase {

    // No mock needed for these tests
    
    private UserListCell userListCell;
    private Stage stage;
    
    // Test data
    private UUID userId;
    private String username;
    private byte[] avatarData;
    private UserStatus status;
    private UserListItem onlineUser;
    private UserListItem awayUser;
    private UserListItem offlineUser;
    private UserListItem userWithAvatar;
    private UserListItem userWithoutAvatar;
    
    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        
        // Create a simple scene with the component
        VBox root = new VBox();
        root.setPrefSize(300, 400);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Create test data
        userId = UUID.randomUUID();
        username = "testuser";
        avatarData = "base64_avatar_data".getBytes();
        status = UserStatus.ONLINE;

        // Create test users with different statuses
        onlineUser = UserListItem.create(userId, username, UserStatus.ONLINE);

        awayUser = UserListItem.create(
            UUID.randomUUID(), "awayuser", UserStatus.AWAY
        );

        offlineUser = UserListItem.create(
            UUID.randomUUID(), "offlineuser", UserStatus.OFFLINE
        );

        // Create users with/without avatar
        userWithAvatar = UserListItem.withAvatar(
            UUID.randomUUID(), "avataruser", UserStatus.ONLINE, "custom_avatar".getBytes()
        );

        userWithoutAvatar = UserListItem.create(
            UUID.randomUUID(), "noavataruser", UserStatus.ONLINE
        );

        // No mock behavior needed for these tests

        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        FxToolkit.cleanupStages();
    }
    
    @Test
    @DisplayName("Should display online user correctly")
    void testDisplayOnlineUser() {
        Platform.runLater(() -> {
            // Create user list cell
            userListCell = new UserListCell();
            
            // Update with online user
            userListCell.updateItem(onlineUser, false);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(userListCell);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify user list cell exists
        assertNotNull(userListCell);
        
        // Verify the item is set correctly
        assertEquals(onlineUser, userListCell.getItem());
        assertFalse(userListCell.isEmpty());
    }
    
    @Test
    @DisplayName("Should display away user correctly")
    void testDisplayAwayUser() {
        Platform.runLater(() -> {
            // Create user list cell
            userListCell = new UserListCell();
            
            // Update with away user
            userListCell.updateItem(awayUser, false);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(userListCell);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify user list cell exists
        assertNotNull(userListCell);
        
        // Verify the item is set correctly
        assertEquals(awayUser, userListCell.getItem());
        assertFalse(userListCell.isEmpty());
    }
    
    @Test
    @DisplayName("Should display offline user correctly")
    void testDisplayOfflineUser() {
        Platform.runLater(() -> {
            // Create user list cell
            userListCell = new UserListCell();
            
            // Update with offline user
            userListCell.updateItem(offlineUser, false);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(userListCell);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify user list cell exists
        assertNotNull(userListCell);
        
        // Verify the item is set correctly
        assertEquals(offlineUser, userListCell.getItem());
        assertFalse(userListCell.isEmpty());
    }
    
    @Test
    @DisplayName("Should display user with avatar correctly")
    void testDisplayUserWithAvatar() {
        Platform.runLater(() -> {
            // Create user list cell
            userListCell = new UserListCell();
            
            // Update with user with avatar
            userListCell.updateItem(userWithAvatar, false);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(userListCell);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify user list cell exists
        assertNotNull(userListCell);
        
        // Verify the item is set correctly
        assertEquals(userWithAvatar, userListCell.getItem());
        assertFalse(userListCell.isEmpty());
    }
    
    @Test
    @DisplayName("Should display user without avatar correctly")
    void testDisplayUserWithoutAvatar() {
        Platform.runLater(() -> {
            // Create user list cell
            userListCell = new UserListCell();
            
            // Update with user without avatar
            userListCell.updateItem(userWithoutAvatar, false);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(userListCell);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify user list cell exists
        assertNotNull(userListCell);
        
        // Verify the item is set correctly
        assertEquals(userWithoutAvatar, userListCell.getItem());
        assertFalse(userListCell.isEmpty());
    }
    
    @Test
    @DisplayName("Should handle empty user correctly")
    void testHandleEmptyUser() {
        Platform.runLater(() -> {
            // Create user list cell
            userListCell = new UserListCell();
            
            // Update with null user
            userListCell.updateItem(null, true);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(userListCell);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify user list cell exists
        assertNotNull(userListCell);
        
        // Verify it's empty
        assertTrue(userListCell.isEmpty());
        assertNull(userListCell.getItem());
    }
    
    @Test
    @DisplayName("Should update user status correctly")
    void testUpdateUserStatus() {
        Platform.runLater(() -> {
            // Create user list cell with online user
            userListCell = new UserListCell();
            userListCell.updateItem(onlineUser, false);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(userListCell);
            
            // Verify initial status
            assertEquals(UserStatus.ONLINE, userListCell.getItem().status());
            
            // Update with same user but offline status
            UserListItem offlineSameUser = onlineUser.withStatus(UserStatus.OFFLINE);
            userListCell.updateItem(offlineSameUser, false);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify status is updated
        assertEquals(UserStatus.OFFLINE, userListCell.getItem().status());
    }
    
    @Test
    @DisplayName("Should get avatar image view correctly")
    void testGetAvatarImageView() {
        Platform.runLater(() -> {
            // Create user list cell
            userListCell = new UserListCell();
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(userListCell);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify avatar image view can be retrieved
        AvatarImageView avatarImageView = userListCell.getAvatarImageView();
        assertNotNull(avatarImageView);
    }
    
    @Test
    @DisplayName("Should get status indicator correctly")
    void testGetStatusIndicator() {
        Platform.runLater(() -> {
            // Create user list cell
            userListCell = new UserListCell();
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(userListCell);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify status indicator can be retrieved
        StatusIndicator statusIndicator = userListCell.getStatusIndicator();
        assertNotNull(statusIndicator);
    }
    
    @Test
    @DisplayName("Should refresh display correctly")
    void testRefreshDisplay() {
        Platform.runLater(() -> {
            // Create user list cell
            userListCell = new UserListCell();
            userListCell.updateItem(onlineUser, false);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(userListCell);
            
            // Refresh display
            userListCell.refreshDisplay();
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify the item is still set correctly
        assertEquals(onlineUser, userListCell.getItem());
        assertFalse(userListCell.isEmpty());
    }
    

}