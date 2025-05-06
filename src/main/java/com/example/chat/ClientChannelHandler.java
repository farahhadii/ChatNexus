package com.example.chat;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import io.netty.channel.ChannelHandler.Sharable;
import java.util.concurrent.CompletableFuture;

@Sharable
public class ClientChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static CompletableFuture<Boolean> userValidationFuture = new CompletableFuture<>();
    private static boolean userLeftDM = false;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        String message = msg.toString(CharsetUtil.UTF_8).trim();

        // Check if this is a username validation response.
        if (message.equals("/userValid")) {
            userValidationFuture.complete(true);
            return;
        } else if (message.equals("/userInvalid")) {
            userValidationFuture.complete(false);
            return;
        }
        if (message.equals("/userLeftDM")) {
            userLeftDM = true;
            return;
        }
        System.out.println(message);
    }

    public static Boolean waitForUserValidationResponse() {
        try {
            return userValidationFuture.get();  // This call will block until the response arrives.
        } catch (Exception e) {
            return false;
        } finally {
            userValidationFuture = new CompletableFuture<>(); // // Reset the future for the next validation request
        }
    }
    public static boolean hasPartnerLeft() {
        return userLeftDM;
    }
    public static void resetPartnerLeft()   {
        userLeftDM = false;
    }


}