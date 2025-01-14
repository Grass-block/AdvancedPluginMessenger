package me.gb2022.apm.remote.protocol.packet.protocol;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.util.BufferUtil;
import me.gb2022.apm.remote.protocol.packet.DeserializedConstructor;
import me.gb2022.apm.remote.protocol.packet.Packet;

public final class P_LoginResult implements Packet {
    private final int result;
    private final String disconnectedMessage;

    private P_LoginResult(int result, String disconnectedMessage) {
        this.result = result;
        this.disconnectedMessage = disconnectedMessage;
    }

    @DeserializedConstructor
    public P_LoginResult(ByteBuf data) {
        this.result = data.readInt();
        this.disconnectedMessage = BufferUtil.readString(data);
    }

    @Override
    public void write(ByteBuf data) {
        data.writeInt(this.result);
        BufferUtil.writeString(data, this.disconnectedMessage);
    }

    public static P_LoginResult succeed() {
        return new P_LoginResult(0, "");
    }

    public static P_LoginResult failed(String disconnectedMessage) {
        return new P_LoginResult(-1, disconnectedMessage);
    }

    public String getDisconnectedMessage() {
        return disconnectedMessage;
    }

    public boolean success() {
        return this.result == 0;
    }
}
