package com.chatv2.server.config;

import com.chatv2.common.exception.ChatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Server configuration loaded from YAML file.
 * Supports validation and default values.
 */
public class ServerProperties {
    private static final Logger log = LoggerFactory.getLogger(ServerProperties.class);

    private final ServerConfig server;
    private final UdpConfig udp;
    private final DatabaseConfig database;
    private final EncryptionConfig encryption;
    private final SessionConfig session;

    private ServerProperties(ServerConfig server, UdpConfig udp, DatabaseConfig database,
                          EncryptionConfig encryption, SessionConfig session) {
        this.server = server;
        this.udp = udp;
        this.database = database;
        this.encryption = encryption;
        this.session = session;
    }

    /**
     * Loads server configuration from YAML file.
     * If file doesn't exist, creates default configuration.
     *
     * @param configPath path to configuration file
     * @return loaded ServerProperties
     * @throws IOException if file cannot be read
     * @throws ChatException if validation fails
     */
    public static ServerProperties load(String configPath) throws IOException {
        Path path = Paths.get(configPath);
        ConfigData configData;

        if (Files.exists(path)) {
            log.info("Loading server configuration from: {}", configPath);
            try (InputStream input = new FileInputStream(path.toFile())) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(input);
                configData = mapToConfigData(data);
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

        ServerProperties properties = configData.toServerProperties();
        log.info("Server configuration loaded successfully");
        log.debug("Server: {}@{}", properties.getServerName(), properties.getHost() + ":" + properties.getPort());
        return properties;
    }

    /**
     * Loads server configuration from default path.
     *
     * @return loaded ServerProperties
     * @throws IOException if file cannot be read
     * @throws ChatException if validation fails
     */
    public static ServerProperties load() throws IOException {
        return load("config/server-config.yaml");
    }

    /**
     * Maps raw YAML data to ConfigData.
     */
    private static ConfigData mapToConfigData(Map<String, Object> data) {
        ConfigData configData = new ConfigData();

        if (data.containsKey("server")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> serverMap = (Map<String, Object>) data.get("server");
            configData.server = new ServerData();
            if (serverMap.containsKey("host")) {
                configData.server.host = String.valueOf(serverMap.get("host"));
            }
            if (serverMap.containsKey("port")) {
                configData.server.port = ((Number) serverMap.get("port")).intValue();
            }
            if (serverMap.containsKey("name")) {
                configData.server.name = String.valueOf(serverMap.get("name"));
            }
        }

        if (data.containsKey("udp")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> udpMap = (Map<String, Object>) data.get("udp");
            configData.udp = new UdpData();
            if (udpMap.containsKey("enabled")) {
                configData.udp.enabled = Boolean.parseBoolean(String.valueOf(udpMap.get("enabled")));
            }
            if (udpMap.containsKey("multicastAddress")) {
                configData.udp.multicastAddress = String.valueOf(udpMap.get("multicastAddress"));
            }
            if (udpMap.containsKey("port")) {
                configData.udp.port = ((Number) udpMap.get("port")).intValue();
            }
            if (udpMap.containsKey("broadcastInterval")) {
                configData.udp.broadcastInterval = ((Number) udpMap.get("broadcastInterval")).intValue();
            }
        }

        if (data.containsKey("database")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dbMap = (Map<String, Object>) data.get("database");
            configData.database = new DatabaseData();
            if (dbMap.containsKey("path")) {
                configData.database.path = String.valueOf(dbMap.get("path"));
            }
            if (dbMap.containsKey("connectionPoolSize")) {
                configData.database.connectionPoolSize = ((Number) dbMap.get("connectionPoolSize")).intValue();
            }
        }

        if (data.containsKey("encryption")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> encMap = (Map<String, Object>) data.get("encryption");
            configData.encryption = new EncryptionData();
            if (encMap.containsKey("required")) {
                configData.encryption.required = Boolean.parseBoolean(String.valueOf(encMap.get("required")));
            }
            if (encMap.containsKey("defaultPlugin")) {
                configData.encryption.defaultPlugin = String.valueOf(encMap.get("defaultPlugin"));
            }
            if (encMap.containsKey("rsaKeySize")) {
                configData.encryption.rsaKeySize = ((Number) encMap.get("rsaKeySize")).intValue();
            }
            if (encMap.containsKey("aesKeySize")) {
                configData.encryption.aesKeySize = ((Number) encMap.get("aesKeySize")).intValue();
            }
        }

        if (data.containsKey("session")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionMap = (Map<String, Object>) data.get("session");
            configData.session = new SessionData();
            if (sessionMap.containsKey("tokenExpirationSeconds")) {
                configData.session.tokenExpirationSeconds = ((Number) sessionMap.get("tokenExpirationSeconds")).intValue();
            }
            if (sessionMap.containsKey("refreshTokenExpirationDays")) {
                configData.session.refreshTokenExpirationDays = ((Number) sessionMap.get("refreshTokenExpirationDays")).intValue();
            }
        }

        return configData;
    }

    /**
     * Saves default configuration to file.
     */
    private static void saveDefaultConfig(Path path, ConfigData configData) throws IOException {
        try {
            Files.createDirectories(path.getParent());
            // Create default YAML content manually to avoid serialization issues
            String yamlContent = """
server:
  host: "0.0.0.0"
  port: 8080
  name: "ChatV2 Server"

udp:
  enabled: false
  multicastAddress: "239.255.255.250"
  port: 9999
  broadcastInterval: 5

database:
  path: "data/chatv2.db"
  connectionPoolSize: 10

encryption:
  required: true
  defaultPlugin: "AES-256-GCM"
  rsaKeySize: 4096
  aesKeySize: 256

session:
  tokenExpirationSeconds: 3600
  refreshTokenExpirationDays: 30
""";
            Files.writeString(path, yamlContent);
            log.info("Default configuration saved to: {}", path);
        } catch (IOException e) {
            log.warn("Failed to save default configuration: {}", e.getMessage());
            throw e;
        }
    }

    // Getters
    public String getHost() {
        return server.getHost();
    }

    public int getPort() {
        return server.getPort();
    }

    public String getServerName() {
        return server.getName();
    }

    public UdpConfig getUdpConfig() {
        return udp;
    }

    public DatabaseConfig getDatabaseConfig() {
        return database;
    }

    public EncryptionConfig getEncryptionConfig() {
        return encryption;
    }

    public SessionConfig getSessionConfig() {
        return session;
    }

    // Configuration classes (replaced records with classes)
    public static class ServerConfig {
        private final String host;
        private final int port;
        private final String name;

        public ServerConfig(String host, int port, String name) {
            if (host == null || host.isBlank()) {
                this.host = "0.0.0.0";
            } else {
                this.host = host;
            }
            if (port < 1 || port > 65535) {
                this.port = 8080;
            } else {
                this.port = port;
            }
            if (name == null || name.isBlank()) {
                this.name = "ChatV2 Server";
            } else {
                this.name = name;
            }
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getName() {
            return name;
        }
    }

    public static class UdpConfig {
        private final boolean enabled;
        private final String multicastAddress;
        private final int port;
        private final int broadcastInterval;

        public UdpConfig(boolean enabled, String multicastAddress, int port, int broadcastInterval) {
            this.enabled = enabled;
            if (multicastAddress == null || multicastAddress.isBlank()) {
                this.multicastAddress = "239.255.255.250";
            } else {
                this.multicastAddress = multicastAddress;
            }
            if (port < 1 || port > 65535) {
                this.port = 9999;
            } else {
                this.port = port;
            }
            if (broadcastInterval < 1) {
                this.broadcastInterval = 5;
            } else {
                this.broadcastInterval = broadcastInterval;
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getMulticastAddress() {
            return multicastAddress;
        }

        public int getPort() {
            return port;
        }

        public int getBroadcastInterval() {
            return broadcastInterval;
        }
    }

    public static class DatabaseConfig {
        private final String path;
        private final int connectionPoolSize;

        public DatabaseConfig(String path, int connectionPoolSize) {
            if (path == null || path.isBlank()) {
                this.path = "data/chatv2.db";
            } else {
                this.path = path;
            }
            if (connectionPoolSize < 1) {
                this.connectionPoolSize = 10;
            } else {
                this.connectionPoolSize = connectionPoolSize;
            }
        }

        public String getPath() {
            return path;
        }

        public int getConnectionPoolSize() {
            return connectionPoolSize;
        }
    }

    public static class EncryptionConfig {
        private final boolean required;
        private final String defaultPlugin;
        private final int rsaKeySize;
        private final int aesKeySize;

        public EncryptionConfig(boolean required, String defaultPlugin, int rsaKeySize, int aesKeySize) {
            this.required = required;
            if (defaultPlugin == null || defaultPlugin.isBlank()) {
                this.defaultPlugin = "AES-256-GCM";
            } else {
                this.defaultPlugin = defaultPlugin;
            }
            if (rsaKeySize < 1024) {
                this.rsaKeySize = 4096;
            } else {
                this.rsaKeySize = rsaKeySize;
            }
            if (aesKeySize < 128) {
                this.aesKeySize = 256;
            } else {
                this.aesKeySize = aesKeySize;
            }
        }

        public boolean isRequired() {
            return required;
        }

        public String getDefaultPlugin() {
            return defaultPlugin;
        }

        public int getRsaKeySize() {
            return rsaKeySize;
        }

        public int getAesKeySize() {
            return aesKeySize;
        }
    }

    public static class SessionConfig {
        private final int tokenExpirationSeconds;
        private final int refreshTokenExpirationDays;

        public SessionConfig(int tokenExpirationSeconds, int refreshTokenExpirationDays) {
            if (tokenExpirationSeconds < 60) {
                this.tokenExpirationSeconds = 3600;
            } else {
                this.tokenExpirationSeconds = tokenExpirationSeconds;
            }
            if (refreshTokenExpirationDays < 1) {
                this.refreshTokenExpirationDays = 30;
            } else {
                this.refreshTokenExpirationDays = refreshTokenExpirationDays;
            }
        }

        public int getTokenExpirationSeconds() {
            return tokenExpirationSeconds;
        }

        public int getRefreshTokenExpirationDays() {
            return refreshTokenExpirationDays;
        }
    }

    // Internal data class for YAML loading
    private static class ServerData {
        String host = "0.0.0.0";
        int port = 8080;
        String name = "ChatV2 Server";
    }

    // Internal data class for YAML loading
    private static class UdpData {
        boolean enabled = false;
        String multicastAddress = "239.255.255.250";
        int port = 9999;
        int broadcastInterval = 5;
    }

    // Internal data class for YAML loading
    private static class DatabaseData {
        String path = "data/chatv2.db";
        int connectionPoolSize = 10;
    }

    // Internal data class for YAML loading
    private static class EncryptionData {
        boolean required = true;
        String defaultPlugin = "AES-256-GCM";
        int rsaKeySize = 4096;
        int aesKeySize = 256;
    }

    // Internal data class for YAML loading
    private static class SessionData {
        int tokenExpirationSeconds = 3600;
        int refreshTokenExpirationDays = 30;
    }

    // Internal data class for YAML loading and mapping
    private static class ConfigData {
        ServerData server;
        UdpData udp;
        DatabaseData database;
        EncryptionData encryption;
        SessionData session;

        public static ConfigData createDefault() {
            ConfigData data = new ConfigData();
            data.server = new ServerData();
            data.udp = new UdpData();
            data.database = new DatabaseData();
            data.encryption = new EncryptionData();
            data.session = new SessionData();
            return data;
        }

        public void validate() {
            if (server == null) {
                throw new ChatException(ChatException.INVALID_REQUEST, "Server configuration is missing");
            }
            if (database == null) {
                throw new ChatException(ChatException.INVALID_REQUEST, "Database configuration is missing");
            }
        }

        public ServerProperties toServerProperties() {
            ServerConfig serverConfig = new ServerConfig(
                server.host, server.port, server.name
            );
            UdpConfig udpConfig = new UdpConfig(
                udp.enabled, udp.multicastAddress, udp.port, udp.broadcastInterval
            );
            DatabaseConfig databaseConfig = new DatabaseConfig(
                database.path, database.connectionPoolSize
            );
            EncryptionConfig encryptionConfig = new EncryptionConfig(
                encryption.required, encryption.defaultPlugin,
                encryption.rsaKeySize, encryption.aesKeySize
            );
            SessionConfig sessionConfig = new SessionConfig(
                session.tokenExpirationSeconds, session.refreshTokenExpirationDays
            );

            return new ServerProperties(
                serverConfig, udpConfig, databaseConfig, encryptionConfig, sessionConfig
            );
        }


    }
}