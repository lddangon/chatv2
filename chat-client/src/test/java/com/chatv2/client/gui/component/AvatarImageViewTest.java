package com.chatv2.client.gui.component;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AvatarImageView component.
 * Tests the display of avatar images, placeholder images, and different sizes.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(ApplicationExtension.class)
class AvatarImageViewTest extends JavaFXTestBase {

    private AvatarImageView avatarImageView;
    private Stage stage;
    
    // Test data
    private byte[] avatarData;
    private String avatarUrl;
    
    @TempDir
    Path tempDir;
    
    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        
        // Create a simple scene with the component
        VBox root = new VBox();
        root.setPrefSize(300, 300);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    
    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Create a simple PNG image for testing
        // This is a 1x1 pixel transparent PNG
        avatarData = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG header
            0x00, 0x00, 0x00, 0x0D, // IHDR chunk length
            0x49, 0x48, 0x44, 0x52, // IHDR
            0x00, 0x00, 0x00, 0x01, // Width: 1
            0x00, 0x00, 0x00, 0x01, // Height: 1
            0x08, 0x06, 0x00, 0x00, 0x00, // Bit depth, color type, compression, filter, interlace
            0x1F, 0x15, (byte) 0xC4, (byte) 0x89, // CRC
            0x00, 0x00, 0x00, 0x0B, // IDAT chunk length
            0x49, 0x44, 0x41, 0x54, // IDAT
            0x08, (byte) 0x99, 0x01, 0x01, 0x00, 0x00, (byte) 0xFE, (byte) 0xFF, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01, // Image data
            (byte) 0x00, 0x00, 0x00, 0x00, // IEND chunk length
            0x49, 0x45, 0x4E, 0x44, // IEND
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82  // CRC
        };

        // Save avatar data to a temporary file
        Path avatarFile = tempDir.resolve("test_avatar.png");
        Files.write(avatarFile, avatarData);
        avatarUrl = avatarFile.toUri().toString();

        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        FxToolkit.cleanupStages();
    }
    
    @Test
    @DisplayName("Should create avatar with default size")
    void testCreateDefaultAvatar() {
        Platform.runLater(() -> {
            // Create avatar image view with default constructor
            avatarImageView = new AvatarImageView();
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(avatarImageView);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify avatar image view exists
        assertNotNull(avatarImageView);
        
        // Verify default size is MEDIUM
        assertEquals(AvatarImageView.AvatarSize.MEDIUM, avatarImageView.getSize());
        
        // Verify no avatar data initially
        assertFalse(avatarImageView.hasAvatar());
        assertNull(avatarImageView.getAvatar());
    }
    
    @Test
    @DisplayName("Should create avatar with specified size")
    void testCreateAvatarWithSize() {
        Platform.runLater(() -> {
            // Create avatar image view with SMALL size
            avatarImageView = new AvatarImageView(AvatarImageView.AvatarSize.SMALL);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(avatarImageView);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify avatar image view exists
        assertNotNull(avatarImageView);
        
        // Verify size is SMALL
        assertEquals(AvatarImageView.AvatarSize.SMALL, avatarImageView.getSize());
    }
    
    @Test
    @DisplayName("Should set and get avatar data correctly")
    void testSetGetAvatarData() {
        Platform.runLater(() -> {
            // Create avatar image view
            avatarImageView = new AvatarImageView();
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(avatarImageView);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify no avatar initially
        assertFalse(avatarImageView.hasAvatar());
        assertNull(avatarImageView.getAvatar());
        
        // Set avatar data
        Platform.runLater(() -> avatarImageView.setAvatar(avatarData));
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify avatar data is set
        assertTrue(avatarImageView.hasAvatar());
        assertArrayEquals(avatarData, avatarImageView.getAvatar());
    }
    
    @Test
    @DisplayName("Should set and get username correctly")
    void testSetGetUsername() {
        Platform.runLater(() -> {
            // Create avatar image view
            avatarImageView = new AvatarImageView();
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(avatarImageView);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Set username
        String testUsername = "testuser";
        Platform.runLater(() -> avatarImageView.setUsername(testUsername));
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify username is set
        assertEquals(testUsername, avatarImageView.getUsername());
    }
    
    @Test
    @DisplayName("Should set and get size correctly")
    void testSetGetSize() {
        Platform.runLater(() -> {
            // Create avatar image view with default size
            avatarImageView = new AvatarImageView();
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(avatarImageView);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify default size
        assertEquals(AvatarImageView.AvatarSize.MEDIUM, avatarImageView.getSize());
        
        // Change size to LARGE
        Platform.runLater(() -> avatarImageView.setSize(AvatarImageView.AvatarSize.LARGE));
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify size is changed
        assertEquals(AvatarImageView.AvatarSize.LARGE, avatarImageView.getSize());
    }
    
    @Test
    @DisplayName("Should handle null size correctly")
    void testHandleNullSize() {
        // Create avatar image view
        avatarImageView = new AvatarImageView();
        
        // Try to set null size - should throw exception
        assertThrows(IllegalArgumentException.class, () -> avatarImageView.setSize(null));
    }
    
    @Test
    @DisplayName("Should clear avatar correctly")
    void testClearAvatar() {
        Platform.runLater(() -> {
            // Create avatar image view with avatar data
            avatarImageView = new AvatarImageView();
            avatarImageView.setAvatar(avatarData);
            
            // Add to scene
            ((VBox) stage.getScene().getRoot()).getChildren().add(avatarImageView);
        });
        
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify avatar is set
        assertTrue(avatarImageView.hasAvatar());
        
        // Clear avatar
        Platform.runLater(() -> avatarImageView.clearAvatar());
        WaitForAsyncUtils.waitForFxEvents();
        
        // Verify avatar is cleared
        assertFalse(avatarImageView.hasAvatar());
        assertNull(avatarImageView.getAvatar());
    }
    
    @Test
    @DisplayName("Should verify size values")
    void testSizeValues() {
        // Verify size enum values
        assertEquals(32, AvatarImageView.AvatarSize.SMALL.getPixels());
        assertEquals(48, AvatarImageView.AvatarSize.MEDIUM.getPixels());
        assertEquals(64, AvatarImageView.AvatarSize.LARGE.getPixels());
    }
}