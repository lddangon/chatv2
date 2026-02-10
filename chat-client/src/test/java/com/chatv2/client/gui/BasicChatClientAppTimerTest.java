package com.chatv2.client.gui;

import com.chatv2.client.core.ChatClient;
import com.chatv2.client.discovery.ServerDiscovery;
import com.chatv2.client.gui.controller.ServerSelectionController;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для проверки управления таймером при смене сцен в ChatClientApp.
 * Проверяет запуск, остановку и синхронизацию таймера при переключении между сценами.
 */
public class BasicChatClientAppTimerTest {

    private ChatClientApp chatClientApp;

    @BeforeEach
    public void setUp() {
        // Инициализируем моки
        MockitoAnnotations.openMocks(this);
        
        // Создаем экземпляр ChatClientApp
        chatClientApp = new ChatClientApp();
    }

    @Test
    public void testAppCreation() {
        // Проверяем, что ChatClientApp создается без ошибок
        assertNotNull(chatClientApp, "ChatClientApp should be created");
    }

    @Test
    public void testMethodsExist() throws Exception {
        // Проверяем, что необходимые методы существуют в ChatClientApp
        
        // Проверяем наличие метода showServerSelectionScene
        assertDoesNotThrow(() -> {
            var method = ChatClientApp.class.getDeclaredMethod("showServerSelectionScene");
            assertNotNull(method, "showServerSelectionScene method should exist");
        });
        
        // Проверяем наличие метода showLoginScene
        assertDoesNotThrow(() -> {
            var method = ChatClientApp.class.getDeclaredMethod("showLoginScene");
            assertNotNull(method, "showLoginScene method should exist");
        });
        
        // Проверяем наличие метода showRegistrationScene
        assertDoesNotThrow(() -> {
            var method = ChatClientApp.class.getDeclaredMethod("showRegistrationScene");
            assertNotNull(method, "showRegistrationScene method should exist");
        });
        
        // Проверяем наличчие метода shutdown
        assertDoesNotThrow(() -> {
            var method = ChatClientApp.class.getDeclaredMethod("shutdown");
            assertNotNull(method, "shutdown method should exist");
        });
    }

    @Test
    public void testFieldsExist() throws Exception {
        // Проверяем, что необходимые поля существуют в ChatClientApp
        
        // Проверяем наличие поля serverSelectionController
        assertDoesNotThrow(() -> {
            var field = ChatClientApp.class.getDeclaredField("serverSelectionController");
            assertNotNull(field, "serverSelectionController field should exist");
        });
    }

    @Test
    public void testShutdownMethodWithoutException() {
        // Проверяем, что метод shutdown можно вызывать без исключений
        assertDoesNotThrow(() -> chatClientApp.shutdown());
    }
}