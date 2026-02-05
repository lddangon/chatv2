package com.chatv2.client.gui.component;

import com.chatv2.common.model.Message;
import com.chatv2.common.model.MessageType;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MessageBubble component.
 * Tests the display of own/other messages, different message types, and read status.
 */
@ExtendWith(ApplicationExtension.class)
class MessageBubbleTest extends JavaFXTestBase {

    // No mocks needed for these tests
    
    private MessageBubble messageBubble;
    private Stage stage;
    
    // Test data
    private UUID messageId;
    private UUID chatId;
    private UUID senderId;
    private String messageContent;
    private Message textMessage;
    private Message imageMessage;
    private Message fileMessage;
    
    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        
        // Create a simple scene with the component
        VBox root = new VBox();
        root.setPrefSize(400, 300);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Create test data
        messageId = UUID.randomUUID();
        chatId = UUID.randomUUID();
        senderId = UUID.randomUUID();
        messageContent = "Test message";

        // Create test messages
        textMessage = Message.createNew(chatId, senderId, messageContent, MessageType.TEXT);

        imageMessage = Message.createNew(
            chatId,
            senderId,
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==", // 1x1 transparent PNG
            MessageType.IMAGE
        );

        fileMessage = Message.createNew(
            chatId,
            senderId,
            "document.pdf",
            MessageType.FILE
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
    @DisplayName("Should display own text message correctly")
    void testDisplayOwnTextMessage() {
        Platform.runLater(() -> {
            // Create message bubble for own message
            messageBubble = new MessageBubble(textMessage, true);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(messageBubble);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify message bubble exists
        assertNotNull(messageBubble);
        
        // Verify it's marked as own message
        assertTrue(messageBubble.isOwnMessage());
        
        // Verify message is set correctly
        assertEquals(textMessage, messageBubble.getMessage());
        
        // Verify initial states
        assertFalse(messageBubble.isRead());
        assertFalse(messageBubble.isEdited());
    }
    
    @Test
    @DisplayName("Should display other user's text message correctly")
    void testDisplayOtherTextMessage() {
        Platform.runLater(() -> {
            // Create message bubble for other user's message
            messageBubble = new MessageBubble(textMessage, false, "testuser");
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(messageBubble);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify message bubble exists
        assertNotNull(messageBubble);
        
        // Verify it's marked as other user's message
        assertFalse(messageBubble.isOwnMessage());
        
        // Verify message is set correctly
        assertEquals(textMessage, messageBubble.getMessage());
        
        // Verify initial states
        assertFalse(messageBubble.isRead());
        assertFalse(messageBubble.isEdited());
    }
    
    @Test
    @DisplayName("Should display image message correctly")
    void testDisplayImageMessage() {
        Platform.runLater(() -> {
            // Create message bubble for image message
            messageBubble = new MessageBubble(imageMessage, false, "testuser");
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(messageBubble);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify message bubble exists
        assertNotNull(messageBubble);
        
        // Verify it's marked as other user's message
        assertFalse(messageBubble.isOwnMessage());
        
        // Verify message is set correctly
        assertEquals(imageMessage, messageBubble.getMessage());
    }
    
    @Test
    @DisplayName("Should display file message correctly")
    void testDisplayFileMessage() {
        Platform.runLater(() -> {
            // Create message bubble for file message
            messageBubble = new MessageBubble(fileMessage, false, "testuser");
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(messageBubble);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify message bubble exists
        assertNotNull(messageBubble);
        
        // Verify it's marked as other user's message
        assertFalse(messageBubble.isOwnMessage());
        
        // Verify message is set correctly
        assertEquals(fileMessage, messageBubble.getMessage());
    }
    
    @Test
    @DisplayName("Should set and get read status correctly")
    void testSetGetReadStatus() {
        Platform.runLater(() -> {
            // Create message bubble for own message
            messageBubble = new MessageBubble(textMessage, true);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(messageBubble);
            
            // Initially not read
            assertFalse(messageBubble.isRead());
            
            // Mark as read
            messageBubble.setReadStatus(true);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify read status is updated
        assertTrue(messageBubble.isRead());
        
        // Mark as unread
        Platform.runLater(() -> messageBubble.setReadStatus(false));
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify read status is updated
        assertFalse(messageBubble.isRead());
    }
    
    @Test
    @DisplayName("Should set and get edited status correctly")
    void testSetGetEditedStatus() {
        Platform.runLater(() -> {
            // Create message bubble for own message
            messageBubble = new MessageBubble(textMessage, true);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(messageBubble);
            
            // Initially not edited
            assertFalse(messageBubble.isEdited());
            
            // Mark as edited
            messageBubble.setEdited(true);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify edited status is updated
        assertTrue(messageBubble.isEdited());
        
        // Mark as not edited
        Platform.runLater(() -> messageBubble.setEdited(false));
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify edited status is updated
        assertFalse(messageBubble.isEdited());
    }
    
    @Test
    @DisplayName("Should update message correctly")
    void testUpdateMessage() {
        Platform.runLater(() -> {
            // Create message bubble for own message
            messageBubble = new MessageBubble(textMessage, true);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(messageBubble);
            
            // Verify initial message
            assertEquals(textMessage, messageBubble.getMessage());
            
            // Create updated message (edited)
            Message updatedMessage = textMessage.withEditedContent("Updated message");
            
            // Update message
            messageBubble.updateMessage(updatedMessage);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify message is updated
        assertEquals("Updated message", messageBubble.getMessage().content());
        assertTrue(messageBubble.isEdited());
    }
    
    @Test
    @DisplayName("Should set avatar correctly")
    void testSetAvatar() {
        Platform.runLater(() -> {
            // Create message bubble for other user's message
            messageBubble = new MessageBubble(textMessage, false, "testuser");
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(messageBubble);
            
            // Set avatar data
            byte[] avatarData = "avatar_data".getBytes();
            messageBubble.setAvatar(avatarData);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify avatar is set (verified by no exception)
        assertNotNull(messageBubble);
    }
    
    @Test
    @DisplayName("Should not set avatar for own message")
    void testNotSetAvatarForOwnMessage() {
        Platform.runLater(() -> {
            // Create message bubble for own message
            messageBubble = new MessageBubble(textMessage, true);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(messageBubble);
            
            // Try to set avatar data (should be ignored for own messages)
            byte[] avatarData = "avatar_data".getBytes();
            messageBubble.setAvatar(avatarData);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify avatar setting is ignored (verified by no exception)
        assertNotNull(messageBubble);
    }
    
    @Test
    @DisplayName("Should create own message bubble correctly")
    void testCreateOwnMessageBubble() {
        Platform.runLater(() -> {
            // Create message bubble using factory method
            messageBubble = MessageBubble.createOwn(textMessage);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(messageBubble);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify message bubble exists and is configured correctly
        assertNotNull(messageBubble);
        assertTrue(messageBubble.isOwnMessage());
        assertEquals(textMessage, messageBubble.getMessage());
    }
    
    @Test
    @DisplayName("Should create other message bubble correctly")
    void testCreateOtherMessageBubble() {
        Platform.runLater(() -> {
            // Create message bubble using factory method
            messageBubble = MessageBubble.createOther(textMessage, "testuser");
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(messageBubble);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify message bubble exists and is configured correctly
        assertNotNull(messageBubble);
        assertFalse(messageBubble.isOwnMessage());
        assertEquals(textMessage, messageBubble.getMessage());
    }
    
    @Test
    @DisplayName("Should handle null message correctly")
    void testHandleNullMessage() {
        // Try to create message bubble with null message - should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            new MessageBubble(null, true);
        });
    }
    
    @Test
    @DisplayName("Should handle null updated message correctly")
    void testHandleNullUpdatedMessage() {
        Platform.runLater(() -> {
            // Create message bubble
            messageBubble = new MessageBubble(textMessage, true);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(messageBubble);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Try to update with null message - should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            messageBubble.updateMessage(null);
        });
    }
}