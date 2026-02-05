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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for JavaFX tests that handles platform initialization
 */
public abstract class JavaFXTestBase extends ApplicationTest {
    
    protected Stage stage;
    
    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        
        // Create a simple scene with component
        VBox root = new VBox();
        root.setPrefSize(300, 300);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    
    @BeforeEach
    public void setUp() throws Exception {
        // Check if JavaFX platform is already initialized
        try {
            if (!Platform.isFxApplicationThread()) {
                CountDownLatch latch = new CountDownLatch(1);
                Platform.startup(() -> latch.countDown());
                assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS), "JavaFX platform should be initialized");
            }
        } catch (IllegalStateException e) {
            // Toolkit already initialized, which is fine
            if (!e.getMessage().contains("Toolkit already initialized")) {
                throw e;
            }
        }
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        FxToolkit.cleanupStages();
    }
}