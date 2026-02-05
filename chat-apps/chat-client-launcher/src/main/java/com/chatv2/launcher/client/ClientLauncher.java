package com.chatv2.launcher.client;

import com.chatv2.client.gui.ChatClientApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client launcher application for GUI mode.
 * Launches the JavaFX-based ChatV2 Client GUI.
 */
public class ClientLauncher {
    private static final Logger log = LoggerFactory.getLogger(ClientLauncher.class);

    public static void main(String[] args) {
        log.info("Starting ChatV2 Client GUI Launcher...");
        log.info("Java Version: {}", System.getProperty("java.version"));

        try {
            // Launch JavaFX application using standard API
            javafx.application.Application.launch(ChatClientApp.class, args);
        } catch (Exception e) {
            log.error("Fatal error starting client GUI", e);
            System.exit(1);
        }
    }

}
