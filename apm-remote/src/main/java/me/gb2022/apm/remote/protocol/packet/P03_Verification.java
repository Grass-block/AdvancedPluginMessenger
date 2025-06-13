package me.gb2022.apm.remote.protocol.packet;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.protocol.ChannelState;
import me.gb2022.simpnet.packet.DeserializedConstructor;
import me.gb2022.simpnet.util.BufferUtil;

public final class P03_Verification implements LoginPacket {
    private final String verification;
    private final long reverseVerificationRef;

    public P03_Verification(long reverseVerificationRef, String verification) {
        this.reverseVerificationRef = reverseVerificationRef;
        this.verification = verification;
    }

    @DeserializedConstructor
    public P03_Verification(ByteBuf buffer) {
        this.reverseVerificationRef = buffer.readLong();
        this.verification = BufferUtil.readString(buffer);
    }

    @Override
    public void write(ByteBuf buffer) {
        buffer.writeLong(this.reverseVerificationRef);
        BufferUtil.writeString(buffer, this.verification);
    }

    public long getReverseVerificationRef() {
        return reverseVerificationRef;
    }

    public String getVerification() {
        return verification;
    }

    @Override
    public ChannelState state() {
        return ChannelState.PRE_LOGIN;
    }
}
