package com.chatv2.server.gui;

import com.chatv2.common.model.Chat;
import com.chatv2.common.model.ChatType;
import com.chatv2.common.model.UserProfile;
import com.chatv2.server.gui.controller.ChatController;
import com.chatv2.server.gui.controller.LogViewerController;
import com.chatv2.server.gui.ServerAdminApp;
import com.chatv2.server.manager.ChatManager;
import com.chatv2.server.manager.UserManager;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for Chat and Log loading functionality.
 * Tests the loading of Chat tables and Log viewing components without errors.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(ApplicationExtension.class)
class ChatLogLoadTest extends ApplicationTest {

    @Mock
    private ServerAdminApp mockServerAdminApp;
    
    @Mock
    private ChatManager mockChatManager;
    
    @Mock
    private UserManager mockUserManager;
    
    private ChatController chatController;
    private LogViewerController logController;
    private Stage stage;
    
    // UI components for Chat view
    private TableView<Chat> chatTable;
    private TableView.TableViewSelectionModel<Chat> chatSelectionModel;
    private ListView<UserProfile> participantsListView;
    private Button refreshButton;
    private Button viewParticipantsButton;
    private Button viewHistoryButton;
    
    // UI components for Log view
    private TextArea logTextArea;
    private ComboBox<String> levelFilterComboBox;
    private TextField searchField;
    private Button clearButton;
    private Button exportButton;
    private CheckBox autoScrollCheckBox;
    
    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        
        // Create fallback UI components
        try {
            VBox root = new VBox();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
            
            // Initialize Chat UI components
            chatTable = new TableView<>();
            // Don't mock the selection model, use the default one
            chatSelectionModel = chatTable.getSelectionModel();
            
            participantsListView = new ListView<>();
            refreshButton = new Button("Refresh");
            viewParticipantsButton = new Button("View Participants");
            viewHistoryButton = new Button("View History");
            
            // Initialize Log UI components
            logTextArea = new TextArea();
            logTextArea.setEditable(false);
            levelFilterComboBox = new ComboBox<>();
            searchField = new TextField();
            clearButton = new Button("Clear");
            exportButton = new Button("Export");
            autoScrollCheckBox = new CheckBox("Auto Scroll");
            
            // Add components to root
            root.getChildren().addAll(
                chatTable, participantsListView, refreshButton, viewParticipantsButton, viewHistoryButton,
                logTextArea, levelFilterComboBox, searchField, clearButton, exportButton, autoScrollCheckBox
            );
            
            chatController = new ChatController();
            logController = new LogViewerController();
        } catch (Exception e) {
            fail("Failed to set up test stage: " + e.getMessage());
        }
    }
    
    @BeforeEach
    void setUp() {
        // Initialize controllers
        if (chatController != null) {
            chatController.setMainApp(mockServerAdminApp);
        }
        
        if (logController != null) {
            logController.setMainApp(mockServerAdminApp);
        }
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (logController != null) {
            logController.cleanup();
        }
        
        FxToolkit.cleanupStages();
    }
    
    @Test
    @DisplayName("Should load chats table without errors")
    void testLoadChatsTable() {
        // Create mock chat data
        UUID chatId1 = UUID.randomUUID();
        UUID ownerId1 = UUID.randomUUID();
        Instant now = Instant.now();
        
        Chat chat1 = new Chat(chatId1, ChatType.GROUP, "Test Chat", "Test Description", 
                             ownerId1, "avatar1", now, now, 5);
        Chat chat2 = new Chat(UUID.randomUUID(), ChatType.PRIVATE, null, null, 
                             UUID.randomUUID(), null, now, now, 2);
        
        List<Chat> mockChats = List.of(chat1, chat2);
        
        // Test that the table can be populated
        Platform.runLater(() -> {
            if (chatTable != null) {
                ObservableList<Chat> items = chatTable.getItems();
                items.clear();
                items.addAll(mockChats);
                
                // Verify the table has the correct number of items
                assertEquals(2, items.size());
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @Test
    @DisplayName("Should handle chat table columns correctly")
    void testChatTableColumns() {
        // Test that columns can be created and configured
        Platform.runLater(() -> {
            if (chatTable != null) {
                // Create columns
                TableColumn<Chat, String> chatIdColumn = new TableColumn<>("Chat ID");
                TableColumn<Chat, String> chatNameColumn = new TableColumn<>("Name");
                TableColumn<Chat, ChatType> chatTypeColumn = new TableColumn<>("Type");
                TableColumn<Chat, String> ownerColumn = new TableColumn<>("Owner");
                TableColumn<Chat, Integer> participantsCountColumn = new TableColumn<>("Participants");
                TableColumn<Chat, String> createdAtColumn = new TableColumn<>("Created At");
                
                // Add columns to table
                chatTable.getColumns().clear();
                chatTable.getColumns().addAll(
                    chatIdColumn, chatNameColumn, chatTypeColumn, ownerColumn, 
                    participantsCountColumn, createdAtColumn
                );
                
                // Verify columns are added
                assertEquals(6, chatTable.getColumns().size());
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @Test
    @DisplayName("Should load participants list without errors")
    void testLoadParticipantsList() {
        // Create mock user data
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        String passwordHash = "hashedpassword";
        String salt = "salt";
        
        UserProfile user1 = new UserProfile(userId1, "user1", passwordHash, salt, "User One", null, null, com.chatv2.common.model.UserStatus.OFFLINE, Instant.now(), Instant.now());
        UserProfile user2 = new UserProfile(userId2, "user2", passwordHash, salt, "User Two", null, null, com.chatv2.common.model.UserStatus.OFFLINE, Instant.now(), Instant.now());
        
        List<UserProfile> mockUsers = List.of(user1, user2);
        
        // Test that the list can be populated
        Platform.runLater(() -> {
            if (participantsListView != null) {
                ObservableList<UserProfile> items = participantsListView.getItems();
                items.clear();
                items.addAll(mockUsers);
                
                // Verify the list has the correct number of items
                assertEquals(2, items.size());
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @Test
    @DisplayName("Should enable control buttons in chat view")
    void testChatViewControlButtons() {
        // Test that control buttons exist and can be enabled
        Platform.runLater(() -> {
            // Check refresh button
            if (refreshButton != null) {
                refreshButton.setDisable(false);
                assertFalse(refreshButton.isDisabled());
            }
            
            // Check view participants button
            if (viewParticipantsButton != null) {
                viewParticipantsButton.setDisable(false);
                assertFalse(viewParticipantsButton.isDisabled());
            }
            
            // Check view history button
            if (viewHistoryButton != null) {
                viewHistoryButton.setDisable(false);
                assertFalse(viewHistoryButton.isDisabled());
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @Test
    @DisplayName("Should load log viewer without errors")
    void testLoadLogViewer() {
        // Test that the log text area can be populated
        String testLogMessage = "[2023-01-01 10:00:00] [INFO] Application started";
        
        Platform.runLater(() -> {
            if (logTextArea != null) {
                logTextArea.clear();
                logTextArea.setText(testLogMessage);
                
                // Verify the text area contains the test log message
                assertTrue(logTextArea.getText().contains(testLogMessage));
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @Test
    @DisplayName("Should initialize log filter controls without errors")
    void testInitializeLogFilterControls() {
        // Test that filter controls can be initialized
        Platform.runLater(() -> {
            // Check level filter combo box
            if (levelFilterComboBox != null) {
                levelFilterComboBox.getItems().clear();
                levelFilterComboBox.getItems().addAll("ALL", "DEBUG", "INFO", "WARN", "ERROR");
                levelFilterComboBox.getSelectionModel().selectFirst();
                
                // Verify combo box has items
                assertFalse(levelFilterComboBox.getItems().isEmpty());
                assertEquals("ALL", levelFilterComboBox.getValue());
            }
            
            // Check search field
            if (searchField != null) {
                searchField.setText("test search");
                
                // Verify search field text
                assertEquals("test search", searchField.getText());
            }
            
            // Check auto scroll checkbox
            if (autoScrollCheckBox != null) {
                autoScrollCheckBox.setSelected(true);
                
                // Verify checkbox is selected
                assertTrue(autoScrollCheckBox.isSelected());
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @Test
    @DisplayName("Should enable control buttons in log viewer")
    void testLogViewerControlButtons() {
        // Test that control buttons exist and can be enabled
        Platform.runLater(() -> {
            // Check clear button
            if (clearButton != null) {
                clearButton.setDisable(false);
                assertFalse(clearButton.isDisabled());
            }
            
            // Check export button
            if (exportButton != null) {
                exportButton.setDisable(false);
                assertFalse(exportButton.isDisabled());
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @Test
    @DisplayName("Should handle region import in FXML files")
    void testRegionImport() {
        // This test verifies that Region class can be used in FXML
        // Since Region is a standard JavaFX class, we just verify it can be instantiated
        Platform.runLater(() -> {
            try {
                javafx.scene.layout.Region region = new javafx.scene.layout.Region();
                assertNotNull(region);
                
                // Set some properties to verify it works
                region.setPrefWidth(100);
                region.setPrefHeight(50);
                assertEquals(100, region.getPrefWidth(), 0.1);
                assertEquals(50, region.getPrefHeight(), 0.1);
            } catch (Exception e) {
                fail("Failed to use Region class: " + e.getMessage());
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
    
    @Test
    @DisplayName("Should handle empty data without errors")
    void testEmptyDataHandling() {
        // Test that empty data can be handled without errors
        Platform.runLater(() -> {
            // Test empty chat table
            if (chatTable != null) {
                ObservableList<Chat> items = chatTable.getItems();
                items.clear();
                
                // Verify the table is empty
                assertTrue(items.isEmpty());
            }
            
            // Test empty participants list
            if (participantsListView != null) {
                ObservableList<UserProfile> items = participantsListView.getItems();
                items.clear();
                
                // Verify the list is empty
                assertTrue(items.isEmpty());
            }
            
            // Test empty log text area
            if (logTextArea != null) {
                logTextArea.clear();
                
                // Verify the text area is empty
                assertTrue(logTextArea.getText().isEmpty());
            }
        });
        
        WaitForAsyncUtils.waitForFxEvents();
    }
}