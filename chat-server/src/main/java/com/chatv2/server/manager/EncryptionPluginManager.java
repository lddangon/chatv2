package com.chatv2.server.manager;

import com.chatv2.encryption.api.EncryptionAlgorithm;
import com.chatv2.encryption.api.EncryptionPlugin;
import com.chatv2.server.storage.plugin.EncryptionPluginRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.security.Key;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

/**
 * Manager for encryption plugin discovery and lifecycle.
 * Loads plugins via SPI and provides access to encryption functionality.
 */
public class EncryptionPluginManager {
    private static final Logger log = LoggerFactory.getLogger(EncryptionPluginManager.class);

    private final Map<String, EncryptionPlugin> plugins = new ConcurrentHashMap<>();
    private String activePluginName;
    private Key activeSessionKey;

    /**
     * Constructor that auto-discovers plugins via SPI.
     */
    public EncryptionPluginManager() {
        loadPlugins();
    }

    /**
     * Loads all encryption plugins via ServiceLoader (SPI).
     */
    private void loadPlugins() {
        try {
            ServiceLoader<EncryptionPlugin> loader = ServiceLoader.load(EncryptionPlugin.class);
            List<EncryptionPlugin> loadedPlugins = new ArrayList<>();

            StreamSupport.stream(loader.spliterator(), false)
                .forEach(plugin -> {
                    plugins.put(plugin.getName(), plugin);
                    loadedPlugins.add(plugin);
                    log.info("Loaded encryption plugin: {} v{}", plugin.getName(), plugin.getVersion());
                });

            if (loadedPlugins.isEmpty()) {
                log.warn("No encryption plugins found via SPI");
            } else {
                // Set first plugin as active by default
                activePluginName = loadedPlugins.get(0).getName();
                log.info("Active encryption plugin set to: {}", activePluginName);
            }
        } catch (Exception e) {
            log.error("Failed to load encryption plugins", e);
        }
    }

    /**
     * Gets a plugin by name.
     *
     * @param name the plugin name
     * @return Optional containing the plugin if found
     */
    public Optional<EncryptionPlugin> getPlugin(String name) {
        return Optional.ofNullable(plugins.get(name));
    }

    /**
     * Gets all loaded plugins.
     *
     * @return unmodifiable map of plugins
     */
    public Map<String, EncryptionPlugin> getAllPlugins() {
        return Collections.unmodifiableMap(plugins);
    }

    /**
     * Gets the currently active plugin.
     *
     * @return the active plugin
     * @throws IllegalStateException if no active plugin is set
     */
    public EncryptionPlugin getActivePlugin() {
        if (activePluginName == null) {
            throw new IllegalStateException("No active encryption plugin set");
        }
        EncryptionPlugin plugin = plugins.get(activePluginName);
        if (plugin == null) {
            throw new IllegalStateException("Active plugin not found: " + activePluginName);
        }
        return plugin;
    }

    /**
     * Sets the active plugin by name.
     *
     * @param pluginName the name of the plugin to activate
     * @throws IllegalArgumentException if plugin not found
     */
    public void setActivePlugin(String pluginName) {
        if (!plugins.containsKey(pluginName)) {
            throw new IllegalArgumentException("Plugin not found: " + pluginName);
        }
        this.activePluginName = pluginName;
        log.info("Active encryption plugin changed to: {}", pluginName);
    }

    /**
     * Gets the active session key for encryption/decryption.
     *
     * @return the active session key
     */
    public Key getActiveSessionKey() {
        return activeSessionKey;
    }

    /**
     * Sets the active session key for encryption/decryption.
     *
     * @param sessionKey the session key
     */
    public void setActiveSessionKey(Key sessionKey) {
        this.activeSessionKey = sessionKey;
        log.debug("Active session key updated");
    }

    /**
     * Checks if encryption is enabled (active plugin and key are set).
     *
     * @return true if encryption is enabled
     */
    public boolean isEncryptionEnabled() {
        return activePluginName != null && activeSessionKey != null;
    }

    /**
     * Gets the name of the active plugin.
     *
     * @return the active plugin name
     */
    public String getActivePluginName() {
        return activePluginName;
    }

    /**
     * Generates a new key using the active plugin.
     *
     * @return CompletableFuture containing the new key
     */
    public java.util.concurrent.CompletableFuture<Key> generateKey() {
        return getActivePlugin().generateKey().thenApply(key -> {
            log.debug("Generated new key using plugin: {}", activePluginName);
            return key;
        });
    }

    /**
     * Encrypts data using the active plugin and session key.
     *
     * @param plaintext the data to encrypt
     * @return CompletableFuture containing encryption result
     */
    public java.util.concurrent.CompletableFuture<com.chatv2.common.crypto.EncryptionResult> encrypt(byte[] plaintext) {
        if (activeSessionKey == null) {
            return java.util.concurrent.CompletableFuture.failedFuture(
                new IllegalStateException("No active session key set")
            );
        }
        return getActivePlugin().encrypt(plaintext, activeSessionKey);
    }

    /**
     * Decrypts data using the active plugin and session key.
     *
     * @param ciphertext the encrypted data
     * @param iv         the initialization vector
     * @param tag        the authentication tag
     * @return CompletableFuture containing decrypted data
     */
    public java.util.concurrent.CompletableFuture<byte[]> decrypt(byte[] ciphertext, byte[] iv, byte[] tag) {
        if (activeSessionKey == null) {
            return java.util.concurrent.CompletableFuture.failedFuture(
                new IllegalStateException("No active session key set")
            );
        }
        return getActivePlugin().decrypt(ciphertext, iv, tag, activeSessionKey);
    }
}
