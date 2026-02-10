package com.chatv2.client.core;

import com.chatv2.common.crypto.CryptoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Secure storage for user credentials.
 * Supports both file-based and system preferences storage.
 */
public class CredentialStorage {
    private static final Logger log = LoggerFactory.getLogger(CredentialStorage.class);

    private static final String CREDENTIALS_FILE = ".chatv2_credentials";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER_ME = "rememberMe";
    private static final String KEY_SERVER_HOST = "serverHost";
    private static final String KEY_SERVER_PORT = "serverPort";
    
    private final boolean useFileStorage;
    private final String storageKey;
    private final byte[] encryptionKey;
    
    /**
     * Creates a new credential storage.
     *
     * @param useFileStorage if true, uses file-based storage; otherwise uses system preferences
     * @param storageKey unique key for this storage instance
     */
    public CredentialStorage(boolean useFileStorage, String storageKey) {
        this.useFileStorage = useFileStorage;
        this.storageKey = storageKey != null && !storageKey.isBlank() ? storageKey : "default";
        this.encryptionKey = deriveEncryptionKey(this.storageKey);
    }
    
    /**
     * Creates a new credential storage with default settings.
     */
    public CredentialStorage() {
        this(true, "default");
    }
    
    /**
     * Stores user credentials.
     *
     * @param username username
     * @param password password (will be encrypted)
     * @param rememberMe whether to remember credentials
     * @param serverHost server host
     * @param serverPort server port
     */
    public void storeCredentials(String username, String password, boolean rememberMe,
                                 String serverHost, int serverPort) {
        try {
            if (!rememberMe) {
                clearCredentials();
                log.debug("Remember me is disabled, clearing credentials");
                return;
            }
            
            // Encrypt password before storage
            String encryptedPassword = CryptoUtils.encryptAES(password, new String(encryptionKey));
            
            if (useFileStorage) {
                storeToFile(username, encryptedPassword, serverHost, serverPort);
            } else {
                storeToPreferences(username, encryptedPassword, serverHost, serverPort);
            }
            
            log.info("Credentials stored successfully for user: {}", username);
        } catch (Exception e) {
            log.error("Failed to store credentials", e);
        }
    }
    
    /**
     * Loads stored user credentials.
     *
     * @return map containing credentials, or empty map if not found
     */
    public Map<String, String> loadCredentials() {
        try {
            Map<String, String> credentials;
            
            if (useFileStorage) {
                credentials = loadFromFile();
            } else {
                credentials = loadFromPreferences();
            }
            
            if (!credentials.isEmpty()) {
                // Decrypt password
                String encryptedPassword = credentials.get(KEY_PASSWORD);
                if (encryptedPassword != null && !encryptedPassword.isBlank()) {
                    try {
                        String decryptedPassword = CryptoUtils.decryptAES(encryptedPassword, new String(encryptionKey));
                        credentials.put(KEY_PASSWORD, decryptedPassword);
                    } catch (Exception e) {
                        log.warn("Failed to decrypt password", e);
                        credentials.remove(KEY_PASSWORD);
                    }
                }
                log.debug("Credentials loaded successfully");
            }
            
            return credentials;
        } catch (Exception e) {
            log.error("Failed to load credentials", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Clears stored credentials.
     */
    public void clearCredentials() {
        try {
            if (useFileStorage) {
                clearFileStorage();
            } else {
                clearPreferencesStorage();
            }
            log.debug("Credentials cleared successfully");
        } catch (Exception e) {
            log.error("Failed to clear credentials", e);
        }
    }
    
    /**
     * Checks if credentials exist.
     *
     * @return true if credentials are stored
     */
    public boolean hasCredentials() {
        return !loadCredentials().isEmpty();
    }
    
    /**
     * Stores credentials to file.
     */
    private void storeToFile(String username, String encryptedPassword, String serverHost, int serverPort) 
            throws Exception {
        Path homeDir = Paths.get(System.getProperty("user.home"));
        Path credentialsPath = homeDir.resolve(CREDENTIALS_FILE);
        
        StringBuilder content = new StringBuilder();
        content.append(KEY_USERNAME).append("=").append(username).append("\n");
        content.append(KEY_PASSWORD).append("=").append(encryptedPassword).append("\n");
        content.append(KEY_SERVER_HOST).append("=").append(serverHost).append("\n");
        content.append(KEY_SERVER_PORT).append("=").append(serverPort).append("\n");
        
        Files.writeString(credentialsPath, content.toString(), StandardCharsets.UTF_8);
    }
    
    /**
     * Loads credentials from file.
     */
    private Map<String, String> loadFromFile() throws Exception {
        Path homeDir = Paths.get(System.getProperty("user.home"));
        Path credentialsPath = homeDir.resolve(CREDENTIALS_FILE);
        
        if (!Files.exists(credentialsPath)) {
            return new HashMap<>();
        }
        
        String content = Files.readString(credentialsPath, StandardCharsets.UTF_8);
        Map<String, String> credentials = new HashMap<>();
        
        for (String line : content.split("\n")) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                credentials.put(parts[0].trim(), parts[1].trim());
            }
        }
        
        return credentials;
    }
    
    /**
     * Clears file storage.
     */
    private void clearFileStorage() throws Exception {
        Path homeDir = Paths.get(System.getProperty("user.home"));
        Path credentialsPath = homeDir.resolve(CREDENTIALS_FILE);
        
        if (Files.exists(credentialsPath)) {
            Files.delete(credentialsPath);
        }
    }
    
    /**
     * Stores credentials to system preferences.
     */
    private void storeToPreferences(String username, String encryptedPassword, String serverHost, int serverPort) {
        Preferences prefs = Preferences.userNodeForPackage(CredentialStorage.class);
        prefs.node(storageKey).put(KEY_USERNAME, username);
        prefs.node(storageKey).put(KEY_PASSWORD, encryptedPassword);
        prefs.node(storageKey).put(KEY_SERVER_HOST, serverHost);
        prefs.node(storageKey).putInt(KEY_SERVER_PORT, serverPort);
        prefs.node(storageKey).putBoolean(KEY_REMEMBER_ME, true);
    }
    
    /**
     * Loads credentials from system preferences.
     */
    private Map<String, String> loadFromPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(CredentialStorage.class);
        Preferences node = prefs.node(storageKey);
        
        Map<String, String> credentials = new HashMap<>();
        String username = node.get(KEY_USERNAME, null);
        String encryptedPassword = node.get(KEY_PASSWORD, null);
        String serverHost = node.get(KEY_SERVER_HOST, null);
        int serverPort = node.getInt(KEY_SERVER_PORT, -1);
        boolean rememberMe = node.getBoolean(KEY_REMEMBER_ME, false);
        
        if (username != null && !username.isBlank() && rememberMe) {
            credentials.put(KEY_USERNAME, username);
            if (encryptedPassword != null && !encryptedPassword.isBlank()) {
                credentials.put(KEY_PASSWORD, encryptedPassword);
            }
            if (serverHost != null && !serverHost.isBlank()) {
                credentials.put(KEY_SERVER_HOST, serverHost);
            }
            if (serverPort > 0) {
                credentials.put(KEY_SERVER_PORT, String.valueOf(serverPort));
            }
        }
        
        return credentials;
    }
    
    /**
     * Clears preferences storage.
     */
    private void clearPreferencesStorage() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(CredentialStorage.class);
            prefs.node(storageKey).removeNode();
        } catch (java.util.prefs.BackingStoreException e) {
            log.error("Failed to clear preferences storage", e);
        }
    }
    
    /**
     * Derives encryption key from storage key.
     */
    private byte[] deriveEncryptionKey(String storageKey) {
        // Simple key derivation - in production, use proper PBKDF2
        String baseKey = "chatv2_credential_storage_" + storageKey;
        byte[] key = new byte[32];
        byte[] baseBytes = baseKey.getBytes(StandardCharsets.UTF_8);
        
        for (int i = 0; i < 32; i++) {
            key[i] = baseBytes[i % baseBytes.length];
        }
        
        return key;
    }
}
