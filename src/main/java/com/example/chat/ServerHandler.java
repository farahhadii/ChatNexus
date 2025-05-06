package com.example.chat;

import com.example.chat.ai.AiService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.CharsetUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.netty.channel.*;

@Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);
    private static final Map<Channel, String> mapChannelToUser = new ConcurrentHashMap<>();
    private static final Map<String, Channel> mapUserToChannel = new ConcurrentHashMap<>();
    private static final ChannelGroup groupChatChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final Map<Channel,String> userDM = new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("New Client Connection");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        Channel exitChannel = ctx.channel();
        String user = mapChannelToUser.remove(exitChannel);

        mapUserToChannel.remove(user);
        userDM.forEach((ch, partnerName) -> {
            if (partnerName.equals(user)) {
                userDM.remove(ch);
                ch.writeAndFlush(Unpooled.copiedBuffer("/userLeftDM\n", CharsetUtil.UTF_8));
                ch.writeAndFlush(Unpooled.copiedBuffer(user + " has disconnected.\n", CharsetUtil.UTF_8));
            }
        });
        userDM.remove(exitChannel);
        groupChatChannels.remove(exitChannel);

        for (Channel ch : groupChatChannels) {
            ch.writeAndFlush(Unpooled.copiedBuffer(
                    user + " has left the group chat.\n",
                    CharsetUtil.UTF_8));
        }
        broadcastGroupChatParticipantList();

        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        String strMsg = in.toString(CharsetUtil.UTF_8).trim();
        Channel channel = ctx.channel();


        // Registration: when a new client connects expects them to enter the username as the first message, expects "Username: <YourName>"
        if (!mapChannelToUser.containsKey(channel)) {
            if (strMsg.startsWith("Username: ")) {
                String user = strMsg.substring("Username: ".length()).trim();
                String keyUser = user.toLowerCase(Locale.ROOT);

                if (user.isEmpty()) {
                    channel.writeAndFlush(Unpooled.copiedBuffer("/userInvalid\n", CharsetUtil.UTF_8));
                    return;
                }

                Channel newUser = mapUserToChannel.putIfAbsent(keyUser, channel); // adds lowercase user to mapUserToChannel
                if (newUser != null) { // if not null, means user already existed, hence it returns the channel
                    channel.writeAndFlush(Unpooled.copiedBuffer("/userInvalid\n", CharsetUtil.UTF_8));
                    return;
                }

                mapChannelToUser.put(channel, user); // adds user to mapChannelToUser
                channel.writeAndFlush(Unpooled.copiedBuffer("/userValid\n", CharsetUtil.UTF_8));
            } else {
                channel.writeAndFlush(Unpooled.copiedBuffer(
                        "You must first send 'Username: <YourName>'!\n", CharsetUtil.UTF_8));
            }
            return;
        }

        String thisUser = mapChannelToUser.get(channel);

        // Join group chat if user sends "1"
        if ("1".equals(strMsg)) {
            if (!groupChatChannels.contains(channel)) {
                groupChatChannels.add(channel);
                channel.writeAndFlush(Unpooled.copiedBuffer(
                        "You have joined the group chat.\n", CharsetUtil.UTF_8));
                for (Channel ch : groupChatChannels) {
                    if (ch != channel) {
                        ch.writeAndFlush(Unpooled.copiedBuffer(
                                thisUser + " has joined.\n", CharsetUtil.UTF_8));
                    }
                }

                // Display updated participant list
                broadcastGroupChatParticipantList();
            } else {
                channel.writeAndFlush(Unpooled.copiedBuffer(
                        "You are already in the group chat!\n", CharsetUtil.UTF_8));
            }
            return;
        }

        // Validate direct message targets
        if (strMsg.startsWith("/user ")) {
            String[] tokens = strMsg.split("\\s+", 2);
            if (tokens.length < 2) {
                channel.writeAndFlush(Unpooled.copiedBuffer(
                        "/userInvalid\n", CharsetUtil.UTF_8));
                return;
            }
            String userPicked = tokens[1].trim();
            if (!mapUserToChannel.containsKey(userPicked) || userPicked.equals(thisUser)) {
                channel.writeAndFlush(Unpooled.copiedBuffer("/userInvalid\n", CharsetUtil.UTF_8));
            } else {
                channel.writeAndFlush(Unpooled.copiedBuffer("/userValid\n", CharsetUtil.UTF_8));
            }
            return;
        }

        // message starting with "/dm"
        if (strMsg.startsWith("/dm")) {
            String[] tokens = strMsg.split("\\s+", 3);
            if (tokens.length < 3) {
                channel.writeAndFlush(Unpooled.copiedBuffer(
                        "Usage: /dm <username> <message>\n", CharsetUtil.UTF_8));
                return;
            }
            String targetUser = tokens[1];
            String message = tokens[2];

            Channel targetChannel = mapUserToChannel.get(targetUser);
            userDM.put(channel       , targetUser);
            userDM.put(targetChannel , thisUser );

            if (targetChannel == null) {
                channel.writeAndFlush(Unpooled.copiedBuffer(
                        "User '" + targetUser + "' not found or not online.\n", CharsetUtil.UTF_8));
                return;
            }

            // Send DM
            targetChannel.writeAndFlush(Unpooled.copiedBuffer(
                    "From " + thisUser + " (DM): " + message + "\n", CharsetUtil.UTF_8));
            channel.writeAndFlush(Unpooled.copiedBuffer(
                    "(You) -> " + targetUser + ": " + message + "\n", CharsetUtil.UTF_8));
            return;
        }

        // 6) If it's a "/group" message, broadcast to group
        if (strMsg.startsWith("/group")) {
            String message = strMsg.substring("/group".length()).trim();
            if (groupChatChannels.contains(channel)) {
                broadcastToGroup(thisUser, message, channel);
            } else {
                channel.writeAndFlush(Unpooled.copiedBuffer(
                        "You are not in the group chat. Type '1' to join.\n",
                        CharsetUtil.UTF_8));
            }
            return;
        }

        // if users picks to talk with gemini
        if ("3".equals(strMsg)) {
            channel.writeAndFlush(Unpooled.copiedBuffer(
                    "You’re now chatting with Gemini.\n" +
                            "Type your question (or '/menu' to return):\n",
                    CharsetUtil.UTF_8));
            return;
        }

        if (strMsg.startsWith("/ai ")) {
            System.out.println("Inside ai\n");
            String prompt = strMsg.substring(4).trim();
            System.out.println(prompt);
            // Run the AI call off the I/O thread
            channel.eventLoop().execute(() -> {
                String reply = null;
                try {
                    reply = AiService.ask(prompt);
                } catch (HttpException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                channel.writeAndFlush(Unpooled.copiedBuffer(
                        "Gemini: " + reply + "\n", CharsetUtil.UTF_8));
            });
            return;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Unexpected error in EchoServerHandler", cause);
        ctx.close();
    }

    // Broadcast a message to all in the group
    private void broadcastToGroup(String sender, String msg, Channel senderChannel) {
        for (Channel ch : groupChatChannels) {
            if (ch != senderChannel) {
                ch.writeAndFlush(Unpooled.copiedBuffer(
                        sender + ": " + msg, CharsetUtil.UTF_8));
            }
        }
    }

    // Lists every user currently in the group, sent to everyone
    private void broadcastGroupChatParticipantList() {
        StringBuilder sb = new StringBuilder("Currently in group chat:\n");
        int count = 0;
        for (Channel ch : groupChatChannels) {
            String userName = mapChannelToUser.get(ch);
            if (userName != null) {
                sb.append("- ").append(userName).append("\n");
                count++;
            }
        }
        if (count == 0) {
            sb.append("None\n");
        }

        String participantList = sb.toString();
        for (Channel ch : groupChatChannels) {
            ch.writeAndFlush(Unpooled.copiedBuffer(participantList, CharsetUtil.UTF_8));
        }
    }
}


