package me.gb2022.apm.remote.protocol;

import me.gb2022.apm.remote.connector.EndpointConnector;
import me.gb2022.apm.remote.connector.ExchangeConnector;
import me.gb2022.apm.remote.protocol.handlers.ClientSessionHandler;
import me.gb2022.apm.remote.protocol.handlers.CommonExceptionHandler;
import me.gb2022.apm.remote.protocol.handlers.HeartBeatHandler;
import me.gb2022.apm.remote.protocol.handlers.ServerSessionHandler;
import me.gb2022.apm.remote.protocol.packet.*;
import me.gb2022.commons.math.SHA;
import me.gb2022.simpnet.channel.NettyChannelInitializer;
import me.gb2022.simpnet.packet.PacketRegistry;

import java.nio.charset.StandardCharsets;

public interface APMProtocol {
    int MAGIC_NUMBER = -1350675722;


    static String generateVerification(byte[] key, long ref) {
        return SHA.getSHA512(new String(key, StandardCharsets.UTF_8) + ref, true);
    }

    static PacketRegistry registryV2() {
        return new PacketRegistry(64, (i) -> {
            i.register(0x00, P00_KeepAlive.class);
            i.register(0x01, P01_LoginRequest.class);
            i.register(0x02, P02_LoginResult.class);
            i.register(0x03, P03_Verification.class);
            i.register(0x04, P04_VerificationResult.class);
            i.register(0x05, P05_ConfirmLogin.class);
            i.register(0x10, P10_ServerLogin.class);
            i.register(0x11, P11_ServerLogout.class);
            i.register(0x20, P20_RawData.class);
        });
    }

    static NettyChannelInitializer client(EndpointConnector c) {
        return new NettyChannelInitializer().config((i) -> {
            i.lengthFrame();
            i.encryption(c.getVerification());
            i.compression(256, 1);
            i.packet(registryV2());
            i.handler(HeartBeatHandler::new);
            i.handler(() -> new ClientSessionHandler(c));
            i.handler(() -> new CommonExceptionHandler(c));
        });
    }

    static NettyChannelInitializer server(ExchangeConnector c) {
        return new NettyChannelInitializer().config((i) -> {
            i.lengthFrame();
            i.encryption(c.getVerification());
            i.compression(256, 1);
            i.packet(registryV2());
            i.handler(HeartBeatHandler::new);
            i.handler(() -> new ServerSessionHandler(c));
            i.handler(() -> new CommonExceptionHandler(c));
        });
    }
}
