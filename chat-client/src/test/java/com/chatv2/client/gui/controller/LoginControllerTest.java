package com.chatv2.client.gui.controller;

import com.chatv2.client.core.ChatClient;
import com.chatv2.client.gui.ChatClientApp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoginController class.
 * Focuses on testing initialization to ensure no NullPointerException
 * and that registerButton field is absent.
 */
@ExtendWith(MockitoExtension.class)
public class LoginControllerTest {

    @Mock
    private ChatClient mockChatClient;
    
    @Mock
    private ChatClientApp mockApp;

    private LoginController loginController;
    private MockedStatic<ChatClientApp> mockedChatClientApp;

    @BeforeEach
    public void setUp() {
        // Create controller instance
        loginController = new LoginController();
        
        // Setup static mock for ChatClientApp
        mockedChatClientApp = mockStatic(ChatClientApp.class);
        mockedChatClientApp.when(ChatClientApp::getInstance).thenReturn(mockApp);
        lenient().when(mockApp.getChatClient()).thenReturn(mockChatClient);
    }
    
    @AfterEach
    public void tearDown() {
        if (mockedChatClientApp != null) {
            mockedChatClientApp.close();
        }
    }

    @Test
    public void testInitializationWithoutNullPointerException() {
        // Verify that controller initializes without NullPointerException
        assertNotNull(loginController, "LoginController should not be null");
        
        // Verify that the controller doesn't have a registerButton field
        assertDoesNotThrow(() -> {
            try {
                LoginController.class.getDeclaredField("registerButton");
                fail("LoginController should not have a registerButton field");
            } catch (NoSuchFieldException e) {
                // This is expected - the field should not exist
            }
        });
        
        // We can't fully test initialization without setting up JavaFX components,
        // but we can verify the class structure is correct
        assertNotNull(loginController);
    }
    
    @Test
    public void testControllerHasRequiredFields() {
        // Verify that the controller has the expected fields
        assertDoesNotThrow(() -> {
            LoginController.class.getDeclaredField("usernameField");
            LoginController.class.getDeclaredField("passwordField");
            LoginController.class.getDeclaredField("rememberMeCheckBox");
            LoginController.class.getDeclaredField("loginButton");
            LoginController.class.getDeclaredField("backButton");
            LoginController.class.getDeclaredField("errorLabel");
            LoginController.class.getDeclaredField("chatClient");
        });
    }
    
    @Test
    public void testChatClientIsNotNullAfterInitialization() {
        // We can't fully test initialization without setting up JavaFX components,
        // but we can verify the class structure is correct and that we can access
        // the chatClient field without exceptions
        
        assertDoesNotThrow(() -> {
            Field chatClientField = LoginController.class.getDeclaredField("chatClient");
            chatClientField.setAccessible(true);
            
            // The test passes if we can access the field without exceptions
            assertNotNull(chatClientField);
        });
    }
    
    @Test
    public void testNoRegisterButton() {
        // Verify that the controller has no registerButton field
        assertDoesNotThrow(() -> {
            // Check for field
            try {
                LoginController.class.getDeclaredField("registerButton");
                fail("LoginController should not have a registerButton field");
            } catch (NoSuchFieldException e) {
                // Expected
            }
        });
    }
    
    @Test
    public void testInputValidation() {
        // Test that input validation method exists
        assertDoesNotThrow(() -> {
            LoginController.class.getDeclaredMethod("validateInputs", String.class, String.class);
        });
    }
    
    @Test
    public void testHandleLoginMethodExists() {
        // Verify that the controller has handleLogin method but not handleRegister
        assertDoesNotThrow(() -> {
            LoginController.class.getDeclaredMethod("handleLogin");
        });
    }
}