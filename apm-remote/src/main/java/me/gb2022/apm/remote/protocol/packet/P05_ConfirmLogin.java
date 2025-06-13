package me.gb2022.apm.remote.protocol.packet;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.protocol.ChannelState;
import me.gb2022.simpnet.packet.DeserializedConstructor;

public final class P05_ConfirmLogin implements LoginPacket{

    @DeserializedConstructor
    public P05_ConfirmLogin(ByteBuf buffer){
    }

    public P05_ConfirmLogin() {
        super();
    }

    @Override
    public ChannelState state() {
        return ChannelState.POST_LOGIN;
    }

    @Override
    public void write(ByteBuf buffer) {
    }
}
