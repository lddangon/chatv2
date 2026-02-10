package com.chatv2.client.network;

import com.chatv2.client.core.ClientConfig;
import com.chatv2.common.protocol.BinaryMessageCodec;
import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.ProtocolMessageType;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for client-server connection using BinaryMessageCodec.
 * Tests the network communication without involving server-side business logic.
 */
class NetworkClientIntegrationTest {

    private static final String TEST_HOST = "localhost";
    private static final int TIMEOUT_SECONDS = 30;
    
    private EventLoopGroup serverBossGroup;
    private EventLoopGroup serverWorkerGroup;
    private Channel serverChannel;
    private NetworkClient client;
    private int serverPort;

    @BeforeEach
    void setUp() throws Exception {
        // Start a simple echo server using BinaryMessageCodec
        serverBossGroup = new NioEventLoopGroup(1);
        serverWorkerGroup = new NioEventLoopGroup();
        
        io.netty.bootstrap.ServerBootstrap serverBootstrap = new io.netty.bootstrap.ServerBootstrap();
        serverBootstrap.group(serverBossGroup, serverWorkerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline()
                        .addLast("messageCodec", new BinaryMessageCodec())
                        .addLast("echoHandler", new io.netty.channel.SimpleChannelInboundHandler<ChatMessage>() {
                            @Override
                            protected void channelRead0(io.netty.channel.ChannelHandlerContext ctx, ChatMessage msg) {
                                // Echo the message back to the client
                                ctx.writeAndFlush(msg);
                            }
                        });
                }
            });
        
        // Bind to a random port
        ChannelFuture bindFuture = serverBootstrap.bind(TEST_HOST, 0).sync();
        serverChannel = bindFuture.channel();
        serverPort = ((io.netty.channel.socket.ServerSocketChannel) serverChannel).localAddress().getPort();
        
        // Create client
        client = new NetworkClient();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Shutdown client
        if (client != null) {
            client.shutdown().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        
        // Shutdown server
        if (serverChannel != null) {
            serverChannel.close().sync();
        }
        if (serverWorkerGroup != null) {
            serverWorkerGroup.shutdownGracefully().sync();
        }
        if (serverBossGroup != null) {
            serverBossGroup.shutdownGracefully().sync();
        }
    }

    @Test
    @DisplayName("Should establish connection between client and server")
    void testClientServerConnection() throws Exception {
        // When - Client connects to server
        client.connect(TEST_HOST, serverPort).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Then - Connection should be established
        assertThat(client.isConnected()).isTrue();
    }

    @Test
    @DisplayName("Should send and receive message through BinaryMessageCodec")
    void testSendMessageAndReceive() throws Exception {
        // Given - Connected client
        client.connect(TEST_HOST, serverPort).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // When - Send a message
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Test message");
        payload.put("sender", "testUser");
        
        String jsonPayload = com.chatv2.common.protocol.MessageCodec.encode(payload);
        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        
        ChatMessage originalMessage = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                (byte) 0x00,
                java.util.UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );
        
        // Send message and wait for response
        CompletableFuture<ChatMessage> responseFuture = client.sendRequest(originalMessage);
        ChatMessage responseMessage = responseFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Then - Response should be received
        assertThat(responseMessage).isNotNull();
        assertThat(responseMessage.getMessageType()).isEqualTo(originalMessage.getMessageType());
        
        // Verify payload is the same
        String responseJson = new String(responseMessage.getPayload(), StandardCharsets.UTF_8);
        Map<String, Object> responsePayload = com.chatv2.common.protocol.MessageCodec.decode(responseJson, Map.class);
        assertThat(responsePayload.get("message")).isEqualTo("Test message");
        assertThat(responsePayload.get("sender")).isEqualTo("testUser");
    }

    @Test
    @DisplayName("Should handle messages with different flags")
    void testMessagesWithFlags() throws Exception {
        // Given - Connected client
        client.connect(TEST_HOST, serverPort).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // When - Send a message with encryption flag
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Encrypted message");
        
        String jsonPayload = com.chatv2.common.protocol.MessageCodec.encode(payload);
        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        
        ChatMessage encryptedMessage = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                ChatMessage.FLAG_ENCRYPTED,
                java.util.UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );
        
        // Send message and wait for response
        CompletableFuture<ChatMessage> responseFuture = client.sendRequest(encryptedMessage);
        ChatMessage responseMessage = responseFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Then - Response should be received with the same flags
        assertThat(responseMessage).isNotNull();
        assertThat(responseMessage.getMessageType()).isEqualTo(encryptedMessage.getMessageType());
        assertThat(responseMessage.isEncrypted()).isTrue();
        assertThat(responseMessage.isUrgent()).isFalse();
    }

    @Test
    @DisplayName("Should disconnect properly")
    void testDisconnection() throws Exception {
        // Given - Connected client
        client.connect(TEST_HOST, serverPort).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(client.isConnected()).isTrue();
        
        // When - Client disconnects
        client.disconnect().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Then - Client should no longer be connected
        assertThat(client.isConnected()).isFalse();
    }
}