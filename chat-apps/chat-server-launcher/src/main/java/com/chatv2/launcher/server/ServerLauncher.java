package com.chatv2.launcher.server;

import com.chatv2.server.core.ChatServer;
import com.chatv2.server.core.ServerConfig;
import com.chatv2.server.gui.ServerAdminApp;
import com.chatv2.server.handler.ServerInitializer;
import com.chatv2.server.storage.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Application;

/**
 * Server launcher application.
 * Supports both headless mode and GUI mode.
 */
public class ServerLauncher {
    private static final Logger log = LoggerFactory.getLogger(ServerLauncher.class);

    private static ChatServer chatServer;
    private static DatabaseManager databaseManager;
    private static boolean guiMode = true;

    public static void main(String[] args) {
        log.info("Starting ChatV2 Server Launcher...");
        log.info("Java Version: {}", System.getProperty("java.version"));

        try {
            // Parse command line arguments
            ServerConfig config = parseConfig(args);

            // Check for --no-gui flag
            for (String arg : args) {
                if ("--no-gui".equals(arg)) {
                    guiMode = false;
                    log.info("Running in headless mode (GUI disabled)");
                    break;
                }
            }

            // Initialize database
            log.info("Initializing database: {}", config.getDatabasePath());
            databaseManager = new DatabaseManager(config.getDatabasePath());

            // Create server initializer
            ServerInitializer initializer = new ServerInitializer(databaseManager);

            // Create server
            log.info("Creating server on {}:{}", config.getHost(), config.getPort());
            chatServer = new ChatServer(config, initializer);

            // Start server
            chatServer.start().thenAccept(v -> {
                log.info("========================================");
                log.info("Server started successfully!");
                log.info("Name: {}", config.getName());
                log.info("Host: {}", config.getHost());
                log.info("Port: {}", config.getPort());
                log.info("Encryption: {}", config.isEncryptionRequired() ? "Enabled" : "Disabled");
                log.info("GUI: {}", guiMode ? "Enabled" : "Disabled");
                log.info("========================================");
            }).exceptionally(ex -> {
                log.error("Failed to start server", ex);
                System.exit(1);
                return null;
            }).join();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutdown hook triggered");
                shutdown();
            }));

            // Launch GUI if enabled
            if (guiMode) {
                try {
                    // IMPORTANT: Set static references for GUI BEFORE launching JavaFX
                    // These must be set before Application.launch() because JavaFX may
                    // start initializing controllers immediately
                    log.info("Setting up GUI dependencies...");
                    ServerAdminApp.setChatServer(chatServer);
                    ServerAdminApp.setDatabaseManager(databaseManager);
                    ServerAdminApp.setServerConfig(config);

                    // Validate that all dependencies are set
                    if (ServerAdminApp.getChatServer() == null) {
                        throw new IllegalStateException("ChatServer not set in ServerAdminApp");
                    }
                    if (ServerAdminApp.getDatabaseManager() == null) {
                        throw new IllegalStateException("DatabaseManager not set in ServerAdminApp");
                    }
                    if (ServerAdminApp.getServerConfig() == null) {
                        throw new IllegalStateException("ServerConfig not set in ServerAdminApp");
                    }
                    log.info("GUI dependencies validated successfully");

                    // Launch JavaFX GUI
                    log.info("Launching GUI application...");
                    Application.launch(ServerAdminApp.class, args);
                } catch (Exception e) {
                    log.error("Failed to launch GUI", e);
                    log.error("GUI launch error: {}", e.getMessage());
                    // Continue running in headless mode
                    log.info("Continuing in headless mode");
                }
            } else {
                // Keep server running in headless mode
                log.info("Server running in headless mode. Press Ctrl+C to stop.");
                keepServerRunning();
            }

        } catch (Exception e) {
            log.error("Fatal error starting server", e);
            System.exit(1);
        }
    }

    /**
     * Keeps the server running in headless mode.
     */
    private static void keepServerRunning() {
        try {
            // Main thread just waits
            while (chatServer.getState() != ChatServer.ServerState.STOPPED) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            log.info("Server thread interrupted");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Performs graceful shutdown.
     */
    private static void shutdown() {
        if (chatServer != null) {
            try {
                chatServer.stop().join();
            } catch (Exception e) {
                log.error("Error during server shutdown", e);
            }
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    /**
     * Parses configuration from command line arguments.
     */
    private static ServerConfig parseConfig(String[] args) {
        String host = "0.0.0.0";
        int port = 8080;
        String name = "ChatV2 Server";
        String databasePath = "data/chat.db";
        boolean encryptionRequired = true;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--host" -> {
                    if (i + 1 < args.length) host = args[++i];
                }
                case "--port" -> {
                    if (i + 1 < args.length) port = Integer.parseInt(args[++i]);
                }
                case "--name" -> {
                    if (i + 1 < args.length) name = args[++i];
                }
                case "--database" -> {
                    if (i + 1 < args.length) databasePath = args[++i];
                }
                case "--no-encryption" -> {
                    encryptionRequired = false;
                }
                case "--help" -> {
                    printHelp();
                    System.exit(0);
                }
            }
        }

        return new ServerConfig(host, port, name, databasePath, 10,
            encryptionRequired, 4096, 256,
            "239.255.255.250", 9999, true, 3600, 3600);
    }

    /**
     * Prints help message.
     */
    private static void printHelp() {
        System.out.println("ChatV2 Server Launcher");
        System.out.println();
        System.out.println("Usage: java -jar chat-server-launcher.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --host <address>      Server host address (default: 0.0.0.0)");
        System.out.println("  --port <port>         Server port (default: 8080)");
        System.out.println("  --name <name>         Server name (default: ChatV2 Server)");
        System.out.println("  --database <path>     Database file path (default: data/chat.db)");
        System.out.println("  --no-encryption       Disable encryption");
        System.out.println("  --no-gui              Run in headless mode (no GUI)");
        System.out.println("  --help                Print this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar chat-server-launcher.jar");
        System.out.println("  java -jar chat-server-launcher.jar --port 9090");
        System.out.println("  java -jar chat-server-launcher.jar --host 192.168.1.100 --port 8080");
        System.out.println("  java -jar chat-server-launcher.jar --no-gui");
    }
}
