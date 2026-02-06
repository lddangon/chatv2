package com.chatv2.server.handler;

import com.chatv2.common.protocol.BinaryMessageCodec;
import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.server.config.ServerProperties;
import com.chatv2.server.manager.*;
import com.chatv2.server.pipeline.EncryptionHandler;
import com.chatv2.server.storage.DatabaseManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * Server channel initializer for setting up the pipeline.
 */
public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    private final DatabaseManager databaseManager;
    private final UserManager userManager;
    private final ChatManager chatManager;
    private final SessionManager sessionManager;
    private final MessageManager messageManager;
    private final EncryptionPluginManager encryptionPluginManager;

    public ServerInitializer(DatabaseManager databaseManager, ServerProperties serverProperties) {
        this.databaseManager = databaseManager;
        this.userManager = new UserManager(databaseManager.createUserRepository());
        this.chatManager = new ChatManager(databaseManager.createChatRepository());
        ServerProperties.SessionConfig sessionConfig = serverProperties != null
            ? serverProperties.getSessionConfig()
            : new ServerProperties.SessionConfig(3600, 30);
        this.sessionManager = new SessionManager(databaseManager.createSessionRepository(), sessionConfig);
        this.messageManager = new MessageManager(databaseManager.createMessageRepository());
        this.encryptionPluginManager = new EncryptionPluginManager();
    }

    /**
     * Constructor with default session configuration.
     */
    public ServerInitializer(DatabaseManager databaseManager) {
        this(databaseManager, null);
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // Add length field decoder (handles packets larger than 10MB)
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(10485760, 16, 4, 0, 0));

        // Add length field prepender (prepends length field before each packet)
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));

        // Add binary message codec (implements PROTOCOL_SPEC.md)
        pipeline.addLast("messageCodec", new BinaryMessageCodec());

        // Add encryption handler (decrypts incoming, encrypts outgoing)
        pipeline.addLast("encryptionHandler", new EncryptionHandler(encryptionPluginManager, sessionManager));

        // Business logic handlers (all work with ChatMessage instead of String)
        pipeline.addLast("authHandler", new AuthHandler(userManager, sessionManager));
        pipeline.addLast("chatHandler", new ChatHandler(chatManager, messageManager, sessionManager));
        pipeline.addLast("messageHandler", new MessageHandler(messageManager, sessionManager));
        pipeline.addLast("sessionHandler", new SessionHandler(sessionManager));
        pipeline.addLast("exceptionHandler", new ExceptionHandler());
    }

    /**
     * Gets user manager.
     */
    public UserManager getUserManager() {
        return userManager;
    }

    /**
     * Gets chat manager.
     */
    public ChatManager getChatManager() {
        return chatManager;
    }

    /**
     * Gets session manager.
     */
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * Gets message manager.
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * Gets encryption plugin manager.
     */
    public EncryptionPluginManager getEncryptionPluginManager() {
        return encryptionPluginManager;
    }

    /**
     * Shuts down all managers.
     */
    public void shutdown() {
        userManager.shutdown();
        chatManager.shutdown();
        sessionManager.shutdown();
        messageManager.shutdown();
    }
}
