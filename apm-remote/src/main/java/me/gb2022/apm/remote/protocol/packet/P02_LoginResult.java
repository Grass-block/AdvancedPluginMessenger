package me.gb2022.apm.remote.protocol.packet;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.protocol.ChannelState;
import me.gb2022.simpnet.packet.DeserializedConstructor;
import me.gb2022.simpnet.util.BufferUtil;

public final class P02_LoginResult implements LoginPacket {
    private final boolean success;
    private final String message;
    private final long verificationRef;

    public P02_LoginResult(boolean success, String message, long verificationRef) {
        this.success = success;
        this.message = message;
        this.verificationRef = verificationRef;
    }

    @DeserializedConstructor
    public P02_LoginResult(ByteBuf buffer) {
        this.success = buffer.readBoolean();
        this.message = BufferUtil.readString(buffer);
        this.verificationRef = buffer.readLong();
    }

    @Override
    public void write(ByteBuf buffer) {
        buffer.writeBoolean(this.success);
        BufferUtil.writeString(buffer, this.message);
        buffer.writeLong(this.verificationRef);
    }

    public String getMessage() {
        return this.message;
    }

    public long getVerificationRef() {
        return this.verificationRef;
    }

    public boolean getResult() {
        return this.success;
    }

    @Override
    public ChannelState state() {
        return ChannelState.PRE_LOGIN;
    }
}
