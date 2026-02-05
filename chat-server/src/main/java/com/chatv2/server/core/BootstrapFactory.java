package com.chatv2.server.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Factory for creating Netty server bootstrap with virtual threads.
 */
public class BootstrapFactory {
    private static final Logger log = LoggerFactory.getLogger(BootstrapFactory.class);

    private final ServerConfig config;
    private final ChannelInitializer<SocketChannel> channelInitializer;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public BootstrapFactory(ServerConfig config, ChannelInitializer<SocketChannel> channelInitializer) {
        this.config = config;
        this.channelInitializer = channelInitializer;
    }

    /**
     * Creates and configures the server bootstrap.
     */
    public ServerBootstrap createBootstrap() {
        log.info("Creating Netty bootstrap with virtual threads");

        // Boss group for accepting connections (platform threads)
        bossGroup = new NioEventLoopGroup(1);

        // Worker group for handling connections (virtual threads)
        workerGroup = new NioEventLoopGroup(0, Thread.ofVirtual().factory());

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .option(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(channelInitializer);

        log.info("Bootstrap configured: host={}, port={}", config.getHost(), config.getPort());

        return bootstrap;
    }

    /**
     * Starts the server.
     */
    public CompletableFuture<Channel> startServer() {
        ServerBootstrap bootstrap = createBootstrap();
        return CompletableFuture.supplyAsync(() -> {
            try {
                serverChannel = bootstrap.bind(config.getHost(), config.getPort())
                    .sync()
                    .channel();
                log.info("Server started on {}:{}", config.getHost(), config.getPort());
                return serverChannel;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Server startup interrupted", e);
            } catch (Exception e) {
                log.error("Failed to start server", e);
                throw new RuntimeException("Failed to start server", e);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Stops the server and shuts down event loop groups.
     */
    public CompletableFuture<Void> stopServer() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Stopping server...");

                if (serverChannel != null) {
                    serverChannel.close().sync();
                }

                if (bossGroup != null) {
                    bossGroup.shutdownGracefully().sync();
                }

                if (workerGroup != null) {
                    workerGroup.shutdownGracefully().sync();
                }

                log.info("Server stopped successfully");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Server shutdown interrupted", e);
            } catch (Exception e) {
                log.error("Error stopping server", e);
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Gets the server channel.
     */
    public Channel getServerChannel() {
        return serverChannel;
    }

    /**
     * Checks if server is running.
     */
    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }
}
