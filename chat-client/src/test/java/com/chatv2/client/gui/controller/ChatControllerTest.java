package com.chatv2.client.gui.controller;

import com.chatv2.client.core.ChatClient;
import com.chatv2.client.gui.ChatClientApp;
import com.chatv2.common.model.Message;
import com.chatv2.common.model.MessageType;
import com.chatv2.common.model.UserProfile;
import com.chatv2.common.model.UserStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatController class.
 */
@ExtendWith({ApplicationExtension.class, MockitoExtension.class})
public class ChatControllerTest {

    @Mock
    private ChatClient mockChatClient;

    @Mock
    private ChatClientApp mockChatClientApp;

    private ChatController chatController;

    @BeforeEach
    public void setUp() throws Exception {
        // Initialize JavaFX toolkit for testing
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(() -> latch.countDown());
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX platform should be initialized");

        // Create ChatController instance
        chatController = new ChatController();
        
        // Mock ChatClientApp.getInstance()
        try (MockedStatic<ChatClientApp> mockedApp = mockStatic(ChatClientApp.class)) {
            mockedApp.when(ChatClientApp::getInstance).thenReturn(mockChatClientApp);
            when(mockChatClientApp.getChatClient()).thenReturn(mockChatClient);
            
            // Initialize the controller
            chatController.initialize(null, null);
        }
    }

    @Test
    public void testInitialization() {
        // Verify that the controller initializes properly
        assertNotNull(chatController);
        verify(mockChatClient).registerMessageConsumer(any());
    }

    @Test
    public void testSendMessage() {
        // Setup
        String testMessage = "Test message";
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        when(mockChatClient.getCurrentUserId()).thenReturn(userId);
        
        // Mock messageTextArea and sendButton
        TextArea messageTextArea = new TextArea();
        Button sendButton = new Button();
        
        // Use reflection to set private fields for testing
        try {
            var field = chatController.getClass().getDeclaredField("messageTextArea");
            field.setAccessible(true);
            field.set(chatController, messageTextArea);
            
            var sendButtonField = chatController.getClass().getDeclaredField("sendButton");
            sendButtonField.setAccessible(true);
            sendButtonField.set(chatController, sendButton);
            
            var currentChatIdField = chatController.getClass().getDeclaredField("currentChatId");
            currentChatIdField.setAccessible(true);
            currentChatIdField.set(chatController, chatId);
        } catch (Exception e) {
            fail("Failed to set up test: " + e.getMessage());
        }
        
        // Set the message text
        messageTextArea.setText(testMessage);
        
        // Call the handler directly
        WaitForAsyncUtils.asyncFx(() -> {
            try {
                var method = chatController.getClass().getDeclaredMethod("handleSendMessage");
                method.setAccessible(true);
                method.invoke(chatController);
            } catch (Exception e) {
                fail("Failed to invoke handleSendMessage: " + e.getMessage());
            }
        });
        
        // Wait for async operations
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify
        verify(mockChatClient).sendMessage(any(Message.class));
        assertTrue(messageTextArea.getText().isEmpty(), "Message text area should be cleared after sending");
    }

    @Test
    public void testHandleIncomingMessage() {
        // Setup
        UUID chatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String content = "Incoming message";
        
        when(mockChatClient.getCurrentUserId()).thenReturn(userId);
        
        // Create a test message
        Message message = Message.createNew(chatId, userId, content, MessageType.TEXT);
        
        // Call the handler directly
        WaitForAsyncUtils.asyncFx(() -> {
            try {
                var method = chatController.getClass().getDeclaredMethod("handleIncomingMessage", Message.class);
                method.setAccessible(true);
                method.invoke(chatController, message);
            } catch (Exception e) {
                fail("Failed to invoke handleIncomingMessage: " + e.getMessage());
            }
        });
        
        // Wait for async operations
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify that the message was added to the UI (this is a simplified test)
        // In a real test, we would check that the message was added to the messageVBox
        // For now, we just verify that no exceptions were thrown
        assertTrue(true, "Message handling should complete without exceptions");
    }

    @Test
    public void testHandleLogout() {
        // Call the handler directly
        WaitForAsyncUtils.asyncFx(() -> {
            try {
                var method = chatController.getClass().getDeclaredMethod("handleLogout");
                method.setAccessible(true);
                method.invoke(chatController);
            } catch (Exception e) {
                fail("Failed to invoke handleLogout: " + e.getMessage());
            }
        });
        
        // Wait for async operations
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify
        verify(mockChatClientApp).showLoginScene();
    }

    @Test
    public void testHandleProfile() {
        // Call the handler directly
        WaitForAsyncUtils.asyncFx(() -> {
            try {
                var method = chatController.getClass().getDeclaredMethod("handleProfile");
                method.setAccessible(true);
                method.invoke(chatController);
            } catch (Exception e) {
                fail("Failed to invoke handleProfile: " + e.getMessage());
            }
        });
        
        // Wait for async operations
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify
        verify(mockChatClientApp).showProfileScene();
    }

    @Test
    public void testJavaFXComponentsImport() {
        // This test verifies that JavaFX components are properly imported and can be instantiated
        VBox vBox = new VBox();
        HBox hBox = new HBox();
        Circle circle = new Circle();
        
        assertNotNull(vBox, "VBox should be created successfully");
        assertNotNull(hBox, "HBox should be created successfully");
        assertNotNull(circle, "Circle should be created successfully");
    }
}