package me.gb2022.apm.remote.protocol.packet;

import me.gb2022.apm.remote.protocol.ChannelState;
import me.gb2022.simpnet.packet.Packet;

public interface LoginPacket extends Packet {

    ChannelState state();


}
