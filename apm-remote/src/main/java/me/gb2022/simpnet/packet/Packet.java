package me.gb2022.simpnet.packet;

import io.netty.buffer.ByteBuf;

public interface Packet {

    void write(ByteBuf data);

}
