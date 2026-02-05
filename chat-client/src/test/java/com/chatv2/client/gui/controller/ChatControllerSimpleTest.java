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
 * Simple unit tests for ChatController class.
 */
@ExtendWith({ApplicationExtension.class, MockitoExtension.class})
public class ChatControllerSimpleTest {

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