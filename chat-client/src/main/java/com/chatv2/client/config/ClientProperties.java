package com.chatv2.client.config;

import com.chatv2.common.exception.ChatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Client configuration loaded from YAML file.
 * Supports validation and default values.
 */
public class ClientProperties {
    private static final Logger log = LoggerFactory.getLogger(ClientProperties.class);

    private final ClientConfig client;
    private final DiscoveryConfig discovery;
    private final ConnectionConfig connection;
    private final EncryptionConfig encryption;
    private final UiConfig ui;

    private ClientProperties(ClientConfig client, DiscoveryConfig discovery,
                          ConnectionConfig connection, EncryptionConfig encryption, UiConfig ui) {
        this.client = client;
        this.discovery = discovery;
        this.connection = connection;
        this.encryption = encryption;
        this.ui = ui;
    }

    /**
     * Loads client configuration from YAML file.
     * If file doesn't exist, creates default configuration.
     *
     * @param configPath path to configuration file
     * @return loaded ClientProperties
     * @throws IOException if file cannot be read
     * @throws ChatException if validation fails
     */
    public static ClientProperties load(String configPath) throws IOException {
        Path path = Paths.get(configPath);
        ConfigData configData;

        if (Files.exists(path)) {
            log.info("Loading client configuration from: {}", configPath);
            try (InputStream input = new FileInputStream(path.toFile())) {
                Yaml yaml = new Yaml(new Constructor(ConfigData.class, new LoaderOptions()));
                configData = yaml.load(input);
            } catch (YAMLException e) {
                log.error("Failed to parse YAML configuration", e);
                throw new ChatException(ChatException.INTERNAL_ERROR,
                    "Invalid YAML configuration: " + e.getMessage(), e);
            }
        } else {
            log.warn("Configuration file not found: {}. Creating default configuration.", configPath);
            configData = ConfigData.createDefault();
            saveDefaultConfig(path, configData);
        }

        // Validate configuration
        configData.validate();

        ClientProperties properties = configData.toClientProperties();
        log.info("Client configuration loaded successfully");
        log.debug("Client: {} v{}", properties.getClientName(), properties.getVersion());
        return properties;
    }

    /**
     * Loads client configuration from default path.
     *
     * @return loaded ClientProperties
     * @throws IOException if file cannot be read
     * @throws ChatException if validation fails
     */
    public static ClientProperties load() throws IOException {
        return load("config/client-config.yaml");
    }

    /**
     * Saves default configuration to file.
     */
    private static void saveDefaultConfig(Path path, ConfigData configData) throws IOException {
        try {
            Files.createDirectories(path.getParent());
            Yaml yaml = new Yaml();
            String yamlContent = yaml.dump(configData);
            Files.writeString(path, yamlContent);
            log.info("Default configuration saved to: {}", path);
        } catch (IOException e) {
            log.warn("Failed to save default configuration: {}", e.getMessage());
            throw e;
        }
    }

    // Getters
    public String getClientName() {
        return client.name();
    }

    public String getVersion() {
        return client.version();
    }

    public DiscoveryConfig getDiscoveryConfig() {
        return discovery;
    }

    public ConnectionConfig getConnectionConfig() {
        return connection;
    }

    public EncryptionConfig getEncryptionConfig() {
        return encryption;
    }

    public UiConfig getUiConfig() {
        return ui;
    }

    // Configuration records
    public record ClientConfig(String name, String version) {
        public ClientConfig {
            if (name == null || name.isBlank()) {
                name = "ChatV2 Client";
            }
            if (version == null || version.isBlank()) {
                version = "1.0.0";
            }
        }
    }

    public record DiscoveryConfig(boolean enabled, String multicastAddress, int port, int timeoutSeconds) {
        public DiscoveryConfig {
            if (multicastAddress == null || multicastAddress.isBlank()) {
                multicastAddress = "239.255.255.250";
            }
            if (port < 1 || port > 65535) {
                port = 9999;
            }
            if (timeoutSeconds < 1) {
                timeoutSeconds = 30;
            }
        }
    }

    public record ConnectionConfig(int reconnectAttempts, int reconnectDelaySeconds, int heartbeatIntervalSeconds) {
        public ConnectionConfig {
            if (reconnectAttempts < 0) {
                reconnectAttempts = 5;
            }
            if (reconnectDelaySeconds < 1) {
                reconnectDelaySeconds = 5;
            }
            if (heartbeatIntervalSeconds < 10) {
                heartbeatIntervalSeconds = 30;
            }
        }
    }

    public record EncryptionConfig(boolean enabled) {
    }

    public record UiConfig(String theme, String language, int avatarSize) {
        public UiConfig {
            if (theme == null || theme.isBlank()) {
                theme = "dark";
            }
            if (language == null || language.isBlank()) {
                language = "en";
            }
            if (avatarSize < 32 || avatarSize > 512) {
                avatarSize = 128;
            }
        }
    }

    // Internal data class for YAML loading
    private static class ConfigData {
        private ClientData client;
        private DiscoveryData discovery;
        private ConnectionData connection;
        private EncryptionData encryption;
        private UiData ui;

        public ConfigData() {
            this.client = new ClientData();
            this.discovery = new DiscoveryData();
            this.connection = new ConnectionData();
            this.encryption = new EncryptionData();
            this.ui = new UiData();
        }

        public static ConfigData createDefault() {
            ConfigData data = new ConfigData();
            data.client = new ClientData();
            data.discovery = new DiscoveryData();
            data.connection = new ConnectionData();
            data.encryption = new EncryptionData();
            data.ui = new UiData();
            return data;
        }

        public void validate() {
            if (client == null) {
                throw new ChatException(ChatException.INVALID_REQUEST, "Client configuration is missing");
            }
        }

        public ClientProperties toClientProperties() {
            ClientConfig clientConfig = new ClientConfig(
                client.name, client.version
            );
            DiscoveryConfig discoveryConfig = new DiscoveryConfig(
                discovery.enabled, discovery.multicastAddress,
                discovery.port, discovery.timeoutSeconds
            );
            ConnectionConfig connectionConfig = new ConnectionConfig(
                connection.reconnectAttempts, connection.reconnectDelaySeconds,
                connection.heartbeatIntervalSeconds
            );
            EncryptionConfig encryptionConfig = new EncryptionConfig(
                encryption.enabled
            );
            UiConfig uiConfig = new UiConfig(
                ui.theme, ui.language, ui.avatarSize
            );

            return new ClientProperties(
                clientConfig, discoveryConfig, connectionConfig, encryptionConfig, uiConfig
            );
        }

        // Inner data classes for YAML mapping
        private static class ClientData {
            String name = "ChatV2 Client";
            String version = "1.0.0";
        }

        private static class DiscoveryData {
            boolean enabled = true;
            String multicastAddress = "239.255.255.250";
            int port = 9999;
            int timeoutSeconds = 30;
        }

        private static class ConnectionData {
            int reconnectAttempts = 5;
            int reconnectDelaySeconds = 5;
            int heartbeatIntervalSeconds = 30;
        }

        private static class EncryptionData {
            boolean enabled = true;
        }

        private static class UiData {
            String theme = "dark";
            String language = "en";
            int avatarSize = 128;
        }
    }
}
