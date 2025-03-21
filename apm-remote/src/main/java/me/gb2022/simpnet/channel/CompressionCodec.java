package me.gb2022.simpnet.channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class CompressionCodec extends ChannelDuplexHandler {
    private final Inflater inflater = new Inflater();
    private final Deflater deflater;
    private final int threshold;

    public CompressionCodec(int threshold, int level) {
        this.threshold = threshold;
        this.deflater = new Deflater(level);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf data)) {
            return;
        }

        if (data.readByte() != 0xF) {
            data.markReaderIndex();
            ctx.fireChannelRead(data);
            return;
        }

        this.inflater.reset();
        this.inflater.setInput(data.nioBuffer());

        data.clear();
        data.writerIndex(0);

        while (!this.inflater.finished()) {
            if (!data.isWritable()) {
                data.ensureWritable(256);
            }

            var produced = this.inflater.inflate(data.nioBuffer(data.writerIndex(), data.writableBytes()));
            data.writerIndex(data.writerIndex() + produced);
        }

        ctx.fireChannelRead(data);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof ByteBuf data)) {
            return;
        }

        if (data.writerIndex() < this.threshold) {
            var content = data.copy();

            data.writeByte(0x0);
            data.writeBytes(content);

            ctx.write(data, promise);
            return;
        }

        this.deflater.reset();
        this.deflater.setInput(data.copy().nioBuffer());

        data.clear();
        data.writerIndex(0);
        data.writeByte(0xF);

        this.deflater.finish();

        while (!this.deflater.finished()) {
            if (!data.isWritable()) {
                data.ensureWritable(256);
            }

            var produced = this.deflater.deflate(data.nioBuffer(data.writerIndex(), data.writableBytes()));
            data.writerIndex(data.writerIndex() + produced);
        }

        ctx.write(data, promise);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        deflater.end();
        inflater.end();
        super.handlerRemoved(ctx);
    }
}
