package com.example.chat;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.channel.*;


public class Client {
    private final String host;
    private final int port;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host, port))
                    .handler(new ChannelInitializer<SocketChannel>() { // Specifies a ChannelInitializer that configures the pipeline for each new client channel by adding the ClientChannelHandler
                        @Override
                        public void initChannel(SocketChannel ch) { // For each new client channel, the ChannelInitializer is invoked to set up the channel’s pipeline with the appropriate handler(s).
                            ch.pipeline().addLast(new ClientChannelHandler());
                        }
                    });

            // Connect to the client
            ChannelFuture f = b.connect().sync();
            Channel channel = f.channel();
            System.out.println("Connected to: " + channel.remoteAddress());

            // Create our console reader
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in)); // accepts messages from the client from the console


            while (true) {
                System.out.print("Enter your username: ");
                String userName = in.readLine();
                if (userName == null) {
                    System.out.println("Exiting.");
                    return;
                }

                if (userName.trim().isEmpty()) { // quick local check
                    System.out.println("Username can’t be empty, try again.");
                    continue;
                }

                // fire the trial name at the server
                channel.writeAndFlush(Unpooled.copiedBuffer(
                        "Username: " + userName + "\n", CharsetUtil.UTF_8));

                // block until the handler tells us whether it worked
                Boolean userValid = ClientChannelHandler.waitForUserValidationResponse();

                if (Boolean.TRUE.equals(userValid)) {
                    System.out.println("Welcome, " + userName + "!");
                    break;
                } else {
                    System.out.println("That name is invalid, try again.");
                }
            }

            boolean shouldExit = false;
            while (!shouldExit) {
                System.out.println("\n--- Main Menu ---");
                System.out.println("1. Group Chat");
                System.out.println("2. Direct (1-on-1) Chat");
                System.out.println("3. My Ai");
                System.out.println("4. Quit");
                System.out.print("Choose an option [1-4]: ");

                String choice = in.readLine();
                if (choice == null) {
                    break;
                }

                switch (choice) {
                    case "1":
                        // if user chooses group chat
                        System.out.println("** Entering Group Chat mode. **");
                        System.out.println("Type '/menu' to return to Main Menu.");
                        channel.writeAndFlush(Unpooled.copiedBuffer(
                                "1", CharsetUtil.UTF_8));
                        while (true) {
                            String line = in.readLine();
                            if (line == null) {
                                shouldExit = true;
                                break;
                            }

                            if ("/menu".equalsIgnoreCase(line)) {
                                break;
                            }

                            if ("quit".equalsIgnoreCase(line)) {
                                shouldExit = true;
                                break;
                            }
                            // send as a group message and sends this message to the server
                            channel.writeAndFlush(Unpooled.copiedBuffer(
                                    "/group " + line + "\n", CharsetUtil.UTF_8));
                        }
                        break;

                    case "2":
                        System.out.println("Type '/menu' to return to Main Menu.");
                        String targetUser = null;
                        boolean validUser = false;

                        while (!validUser) {
                            System.out.println("Enter the name you want to DM: ");
                            targetUser = in.readLine();
                            channel.writeAndFlush(Unpooled.copiedBuffer("/user " + targetUser + "\n", CharsetUtil.UTF_8));

                            // Blocking call waiting for the server's response.
                            Boolean userValidated = ClientChannelHandler.waitForUserValidationResponse();
                            if (Boolean.TRUE.equals(userValidated)) {
                                validUser = true;
                            } else {
                                System.out.println("Invalid user. Please try again.");
                            }
                        }

                        while (true) {
                            String line = in.readLine();
                            if (line == null) {
                                shouldExit = true;
                                break;
                            }
                            if ("/menu".equalsIgnoreCase(line)) {
                                break;
                            }
                            if ("quit".equalsIgnoreCase(line)) {
                                shouldExit = true;
                                break;
                            }
                            // Send the direct message.
                            channel.writeAndFlush(Unpooled.copiedBuffer("/dm " + targetUser + " " + line + "\n", CharsetUtil.UTF_8));
                        }
                        break;

                    case "3":
                        channel.writeAndFlush(Unpooled.copiedBuffer("3", CharsetUtil.UTF_8));
                        while (true) {
                            String line = in.readLine();
                            if (line == null) {
                                shouldExit = true;
                                break;
                            }
                            if ("/menu".equalsIgnoreCase(line)) {
                                break;
                            }
                            if ("quit".equalsIgnoreCase(line)) {
                                shouldExit = true;
                                break;
                            }
                            channel.writeAndFlush(Unpooled.copiedBuffer(
                                    "/ai " + line + "\n", CharsetUtil.UTF_8));
                        }
                        break;

                    case "4":
                        shouldExit = true;
                        break;

                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            }

            // Close the connection once we're done
            System.out.println("Closing client connection!");
            channel.close().sync();

        } finally {
            group.shutdownGracefully().sync();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: " + Client.class.getSimpleName() + " <host> <port>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        new Client(host, port).start();
    }
}