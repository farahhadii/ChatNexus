package com.example.chat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.net.InetSocketAddress;

// Used to bind the server to the port to listen for connection requests

public class Server {
    private final int port;

    public Server(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: " + Server.class.getSimpleName() + " <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        new Server(port).start();
    }

    public void start() throws Exception {
        final ServerHandler serverHandler = new ServerHandler();
        // Configure the EventLoopGroup for handling channels (multiple channels) concurrently
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // Create the ServerBootstrap which instantiates and sets up the server
            ServerBootstrap server = new ServerBootstrap();
            server.group(group)
                    .channel(NioServerSocketChannel.class)
                    // Set the socket address to the configured port
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() { // For each new client connection, the server uses a ChannelInitializer to create a pipeline for that connection and adds the necessary handler(s) to it.
                        @Override
                        public void initChannel(SocketChannel ch) { // For each new client connection, a channel is created that has a pipeline where we add our serverHandler to it for processing incoming and outgoing events
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(serverHandler);
                        }
                    });

            ChannelFuture f = server.bind().sync(); // Binds the server and waits until binding completes
            InetSocketAddress address = (InetSocketAddress) f.channel().localAddress();
            System.out.println("Server started and listening on port " + address.getPort());

            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}