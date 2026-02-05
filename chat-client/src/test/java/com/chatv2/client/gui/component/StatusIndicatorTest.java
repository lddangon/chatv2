package com.chatv2.client.gui.component;

import com.chatv2.common.model.UserStatus;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for StatusIndicator component.
 * Tests the display of different user statuses and sizes.
 */
@ExtendWith(ApplicationExtension.class)
class StatusIndicatorTest extends JavaFXTestBase {

    private StatusIndicator statusIndicator;
    private Stage stage;
    
    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        
        // Create a simple scene with the component
        VBox root = new VBox();
        root.setPrefSize(200, 200);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    
    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        FxToolkit.cleanupStages();
    }
    
    @Test
    @DisplayName("Should create status indicator with default size")
    void testCreateDefaultStatusIndicator() {
        Platform.runLater(() -> {
            // Create status indicator with default constructor
            statusIndicator = new StatusIndicator();
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(statusIndicator);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify status indicator exists
        assertNotNull(statusIndicator);
        
        // Verify default size is MEDIUM
        assertEquals(StatusIndicator.StatusSize.MEDIUM, statusIndicator.getSize());
        
        // Verify default status is OFFLINE
        assertEquals(UserStatus.OFFLINE, statusIndicator.getStatus());
        
        // Verify animation is disabled by default
        assertFalse(statusIndicator.isAnimated());
    }
    
    @Test
    @DisplayName("Should create status indicator with specified size")
    void testCreateStatusIndicatorWithSize() {
        Platform.runLater(() -> {
            // Create status indicator with SMALL size
            statusIndicator = new StatusIndicator(StatusIndicator.StatusSize.SMALL);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(statusIndicator);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify status indicator exists
        assertNotNull(statusIndicator);
        
        // Verify size is SMALL
        assertEquals(StatusIndicator.StatusSize.SMALL, statusIndicator.getSize());
    }
    
    @Test
    @DisplayName("Should create status indicator with MEDIUM size")
    void testCreateMediumStatusIndicator() {
        Platform.runLater(() -> {
            // Create status indicator with MEDIUM size
            statusIndicator = new StatusIndicator(StatusIndicator.StatusSize.MEDIUM);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(statusIndicator);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify status indicator exists
        assertNotNull(statusIndicator);
        
        // Verify size is MEDIUM
        assertEquals(StatusIndicator.StatusSize.MEDIUM, statusIndicator.getSize());
    }
    
    @Test
    @DisplayName("Should create status indicator with LARGE size")
    void testCreateLargeStatusIndicator() {
        Platform.runLater(() -> {
            // Create status indicator with LARGE size
            statusIndicator = new StatusIndicator(StatusIndicator.StatusSize.LARGE);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(statusIndicator);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify status indicator exists
        assertNotNull(statusIndicator);
        
        // Verify size is LARGE
        assertEquals(StatusIndicator.StatusSize.LARGE, statusIndicator.getSize());
    }
    
    @Test
    @DisplayName("Should set and get status correctly")
    void testSetGetStatus() {
        Platform.runLater(() -> {
            // Create status indicator
            statusIndicator = new StatusIndicator();
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(statusIndicator);
            
            // Set status to ONLINE
            statusIndicator.setStatus(UserStatus.ONLINE);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify status is set
        assertEquals(UserStatus.ONLINE, statusIndicator.getStatus());
        
        // Set status to AWAY
        Platform.runLater(() -> statusIndicator.setStatus(UserStatus.AWAY));
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify status is updated
        assertEquals(UserStatus.AWAY, statusIndicator.getStatus());
    }
    
    @Test
    @DisplayName("Should handle null status correctly")
    void testHandleNullStatus() {
        // Create status indicator
        statusIndicator = new StatusIndicator();
        
        // Try to set null status - should throw exception
        assertThrows(IllegalArgumentException.class, () -> statusIndicator.setStatus(null));
    }
    
    @Test
    @DisplayName("Should set and get size correctly")
    void testSetGetSize() {
        Platform.runLater(() -> {
            // Create status indicator with default size
            statusIndicator = new StatusIndicator();
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(statusIndicator);
            
            // Set size to LARGE
            statusIndicator.setSize(StatusIndicator.StatusSize.LARGE);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify size is set
        assertEquals(StatusIndicator.StatusSize.LARGE, statusIndicator.getSize());
    }
    
    @Test
    @DisplayName("Should handle null size correctly")
    void testHandleNullSize() {
        // Create status indicator
        statusIndicator = new StatusIndicator();
        
        // Try to set null size - should throw exception
        assertThrows(IllegalArgumentException.class, () -> statusIndicator.setSize(null));
    }
    
    @Test
    @DisplayName("Should set and get animated correctly")
    void testSetGetAnimated() {
        Platform.runLater(() -> {
            // Create status indicator
            statusIndicator = new StatusIndicator();
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(statusIndicator);
            
            // Set animated to true
            statusIndicator.setAnimated(true);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify animation is enabled
        assertTrue(statusIndicator.isAnimated());
        
        // Set animated to false
        Platform.runLater(() -> statusIndicator.setAnimated(false));
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify animation is disabled
        assertFalse(statusIndicator.isAnimated());
    }
    
    @Test
    @DisplayName("Should verify size values")
    void testSizeValues() {
        // Verify size enum values
        assertEquals(8, StatusIndicator.StatusSize.SMALL.getPixels());
        assertEquals(12, StatusIndicator.StatusSize.MEDIUM.getPixels());
        assertEquals(16, StatusIndicator.StatusSize.LARGE.getPixels());
    }
    
    @Test
    @DisplayName("Should update status with animation")
    void testUpdateStatusWithAnimation() {
        Platform.runLater(() -> {
            // Create status indicator
            statusIndicator = new StatusIndicator();
            statusIndicator.setAnimated(true);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(statusIndicator);
            
            // Set status to ONLINE
            statusIndicator.setStatus(UserStatus.ONLINE);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify status is set and animation is enabled
        assertEquals(UserStatus.ONLINE, statusIndicator.getStatus());
        assertTrue(statusIndicator.isAnimated());
    }
}