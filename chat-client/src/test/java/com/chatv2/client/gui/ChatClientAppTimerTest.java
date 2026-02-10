package com.chatv2.client.gui;

import com.chatv2.client.core.ChatClient;
import com.chatv2.client.discovery.ServerDiscovery;
import com.chatv2.client.gui.controller.ServerSelectionController;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Тесты для проверки управления таймером при смене сцен в ChatClientApp.
 * Проверяет запуск, остановку и синхронизацию таймера при переключении между сценами.
 */
@ExtendWith({ApplicationExtension.class, MockitoExtension.class})
public class ChatClientAppTimerTest {

    @Mock
    private ChatClient mockChatClient;

    @Mock
    private ServerDiscovery mockServerDiscovery;

    @Mock
    private ServerSelectionController mockServerSelectionController;

    @Mock
    private Stage mockStage;

    private ChatClientApp chatClientApp;
    private Timer capturedTimer;

    @BeforeEach
    public void setUp() throws Exception {
        // Создаем экземпляр ChatClientApp
        chatClientApp = new ChatClientApp();
        
        // Используем reflection для установки моков
        var appClass = ChatClientApp.class;
        
        var chatClientField = appClass.getDeclaredField("chatClient");
        chatClientField.setAccessible(true);
        chatClientField.set(chatClientApp, mockChatClient);
        
        var serverDiscoveryField = appClass.getDeclaredField("serverDiscovery");
        serverDiscoveryField.setAccessible(true);
        serverDiscoveryField.set(chatClientApp, mockServerDiscovery);
        
        var serverSelectionControllerField = appClass.getDeclaredField("serverSelectionController");
        serverSelectionControllerField.setAccessible(true);
        serverSelectionControllerField.set(chatClientApp, mockServerSelectionController);
        
        var primaryStageField = appClass.getDeclaredField("primaryStage");
        primaryStageField.setAccessible(true);
        primaryStageField.set(chatClientApp, mockStage);
        
        // Мокаем поведение ServerSelectionController
        when(mockServerSelectionController.isAutoDiscoveryMode()).thenReturn(true);
    }

    @Test
    public void testTimerStartedWhenShowingServerSelection() {
        // Проверяем, что таймер запускается при показе ServerSelectionScene
        // в режиме авто-обнаружения
        
        // Вызываем метод показа ServerSelectionScene
        chatClientApp.showServerSelectionScene();
        
        // Проверяем, что был вызван метод isAutoDiscoveryMode
        verify(mockServerSelectionController).isAutoDiscoveryMode();
        
        // Проверяем, что был вызван метод startAutoRefresh
        verify(mockServerSelectionController).startAutoRefresh();
    }

    @Test
    public void testTimerNotStartedInManualMode() {
        // Проверяем, что таймер не запускается в ручном режиме
        
        // Устанавливаем ручной режим
        when(mockServerSelectionController.isAutoDiscoveryMode()).thenReturn(false);
        
        // Вызываем метод показа ServerSelectionScene
        chatClientApp.showServerSelectionScene();
        
        // Проверяем, что был вызван метод isAutoDiscoveryMode
        verify(mockServerSelectionController).isAutoDiscoveryMode();
        
        // Проверяем, что НЕ был вызван метод startAutoRefresh
        verify(mockServerSelectionController, never()).startAutoRefresh();
    }

    @Test
    public void testTimerStoppedWhenSwitchingToLoginScene() {
        // Проверяем, что таймер останавливается при переключении на LoginScene
        
        // Вызываем метод показа LoginScene
        chatClientApp.showLoginScene();
        
        // Проверяем, что был вызван метод stopAutoRefresh
        verify(mockServerSelectionController).stopAutoRefresh();
    }

    @Test
    public void testTimerStoppedWhenSwitchingToRegistrationScene() {
        // Проверяем, что таймер останавливается при переключении на RegistrationScene
        
        // Вызываем метод показа RegistrationScene
        chatClientApp.showRegistrationScene();
        
        // Проверяем, что был вызван метод stopAutoRefresh
        verify(mockServerSelectionController).stopAutoRefresh();
    }

    @Test
    public void testTimerNotStoppedWhenSwitchingToOtherScenes() {
        // Проверяем, что таймер не останавливается при переключении на сцены,
        // которые не должны его останавливать
        
        // Вызываем методы показа других сцен
        chatClientApp.showChatScene();
        chatClientApp.showProfileScene();
        
        // Проверяем, что НЕ был вызван метод stopAutoRefresh
        verify(mockServerSelectionController, never()).stopAutoRefresh();
    }

    @Test
    public void testTimerStoppedInShutdown() {
        // Проверяем, что таймер останавливается при выключении приложения
        
        // Вызываем метод shutdown
        chatClientApp.shutdown();
        
        // Проверяем, что был вызван метод cleanup у контроллера
        verify(mockServerSelectionController).cleanup();
    }

    @Test
    public void testNullControllerHandling() {
        // Проверяем обработку случая, когда контроллер равен null
        
        try {
            // Устанавливаем контроллер в null
            var serverSelectionControllerField = ChatClientApp.class.getDeclaredField("serverSelectionController");
            serverSelectionControllerField.setAccessible(true);
            serverSelectionControllerField.set(chatClientApp, null);
            
            // Вызываем методы - не должно быть исключений
            assertDoesNotThrow(() -> chatClientApp.showServerSelectionScene());
            assertDoesNotThrow(() -> chatClientApp.showLoginScene());
            assertDoesNotThrow(() -> chatClientApp.showRegistrationScene());
            assertDoesNotThrow(() -> chatClientApp.shutdown());
        } catch (Exception e) {
            fail("Exception should not be thrown when controller is null: " + e.getMessage());
        }
    }

    @Test
    public void testTimerStateSynchronization() {
        // Проверяем синхронизацию состояния таймера при переключении между сценами
        
        // Начинаем с режима авто-обнаружения
        when(mockServerSelectionController.isAutoDiscoveryMode()).thenReturn(true);
        chatClientApp.showServerSelectionScene();
        verify(mockServerSelectionController).startAutoRefresh();
        
        // Переключаемся на LoginScene - таймер должен остановиться
        chatClientApp.showLoginScene();
        verify(mockServerSelectionController).stopAutoRefresh();
        
        // Возвращаемся на ServerSelectionScene - таймер должен запуститься снова
        chatClientApp.showServerSelectionScene();
        verify(mockServerSelectionController, times(2)).startAutoRefresh();
        
        // Переключаемся на RegistrationScene - таймер должен остановиться
        chatClientApp.showRegistrationScene();
        verify(mockServerSelectionController, times(2)).stopAutoRefresh();
    }
}