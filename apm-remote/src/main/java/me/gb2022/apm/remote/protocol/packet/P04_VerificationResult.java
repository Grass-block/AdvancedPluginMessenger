package me.gb2022.apm.remote.protocol.packet;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.protocol.ChannelState;
import me.gb2022.simpnet.packet.DeserializedConstructor;
import me.gb2022.simpnet.util.BufferUtil;

import java.util.Set;

public final class P04_VerificationResult implements LoginPacket {
    private final String reverseVerification;
    private final Set<String> networkMembers;

    public P04_VerificationResult(String reverseVerification, Set<String> networkMembers) {
        this.networkMembers = networkMembers;
        this.reverseVerification = reverseVerification;
    }

    @DeserializedConstructor
    public P04_VerificationResult(ByteBuf buffer) {
        this.reverseVerification = BufferUtil.readString(buffer);
        this.networkMembers = Set.of(BufferUtil.readString(buffer).split(";;"));
    }

    @Override
    public void write(ByteBuf buffer) {
        BufferUtil.writeString(buffer, this.reverseVerification);
        BufferUtil.writeString(buffer, String.join(";;", this.networkMembers));
    }

    public Set<String> getNetworkMembers() {
        return networkMembers;
    }

    public String getReverseVerification() {
        return reverseVerification;
    }

    @Override
    public ChannelState state() {
        return ChannelState.POST_LOGIN;
    }
}
