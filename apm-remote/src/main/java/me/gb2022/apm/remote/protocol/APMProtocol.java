package me.gb2022.apm.remote.protocol;

import me.gb2022.simpnet.channel.NettyChannelInitializer;
import me.gb2022.simpnet.packet.PacketRegistry;
import me.gb2022.simpnet.util.MessageVerification;

public interface APMProtocol {
    PacketRegistry REGISTRY = new PacketRegistry(512, (i) -> {
        i.register(0x00, P_Login.class);
        i.register(0x01, P_LoginResult.class);
        i.register(0x02, P_Logout.class);
        i.register(0x03, P_ServerLogin.class);
        i.register(0x04, P_ServerLogout.class);

        i.register(0x10, D_Raw.class);
    });
    int MAGIC_NUMBER = -1350675722;

    static PacketRegistry registry() {
        return REGISTRY;
    }

    static NettyChannelInitializer channelBuilder(MessageVerification verification) {
        return new NettyChannelInitializer().config((i) -> {
            i.lengthFrame();
            i.encryption(verification);
            i.compression(0, 0);
            i.packet(registry());
        });
    }
}
