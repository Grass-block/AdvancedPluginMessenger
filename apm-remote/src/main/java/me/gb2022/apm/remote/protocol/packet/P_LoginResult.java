package me.gb2022.apm.remote.protocol.packet;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.util.BufferUtil;

public final class P_LoginResult implements Packet {
    private final int result;
    private final String[] servers;
    private final String disconnectedMessage;

    private P_LoginResult(int result, String disconnectedMessage, String[] servers) {
        this.result = result;
        this.servers = servers;
        this.disconnectedMessage = disconnectedMessage;
    }

    @DeserializedConstructor
    public P_LoginResult(ByteBuf data) {
        this.result = data.readInt();
        this.disconnectedMessage = BufferUtil.readString(data);
        this.servers = BufferUtil.readString(data).split(";");
    }

    public static P_LoginResult succeed(String... servers) {
        return new P_LoginResult(0, "", servers);
    }

    public static P_LoginResult failed(String disconnectedMessage) {
        return new P_LoginResult(-1, disconnectedMessage, new String[0]);
    }

    @Override
    public void write(ByteBuf data) {
        data.writeInt(this.result);
        BufferUtil.writeString(data, this.disconnectedMessage);
        BufferUtil.writeString(data, String.join(";", this.servers));
    }

    public String disconnectedMessage() {
        return disconnectedMessage;
    }

    public boolean success() {
        return this.result == 0;
    }

    public String[] servers() {
        return servers;
    }
}
