package me.gb2022.simpnet.channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import me.gb2022.simpnet.util.MessageVerification;

public final class EncryptionCodec extends ChannelDuplexHandler {
    private final MessageVerification verification;

    public EncryptionCodec(MessageVerification verification) {
        this.verification = verification;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf data)) {
            return;
        }

        this.verification.decryptStream(data);
        ctx.fireChannelRead(data);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (!(msg instanceof ByteBuf data)) {
            return;
        }

        this.verification.encryptStream(data);
        ctx.write(data, promise);
    }
}
