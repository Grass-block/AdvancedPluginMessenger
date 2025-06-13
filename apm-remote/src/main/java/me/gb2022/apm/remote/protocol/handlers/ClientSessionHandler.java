package me.gb2022.apm.remote.protocol.handlers;

import io.netty.channel.ChannelHandlerContext;
import me.gb2022.apm.remote.connector.EndpointConnector;
import me.gb2022.apm.remote.protocol.APMProtocol;
import me.gb2022.apm.remote.protocol.ChannelState;
import me.gb2022.apm.remote.protocol.packet.*;
import me.gb2022.simpnet.packet.Packet;
import me.gb2022.simpnet.packet.PacketInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

public final class ClientSessionHandler extends PacketInboundHandler {
    public final Logger logger;

    private final EndpointConnector connector;
    private final String id;
    private final byte[] key;
    private long revVerifyRef;

    private ChannelState state = null;

    public ClientSessionHandler(EndpointConnector connector) {
        this.logger = LogManager.getLogger("APM/CLSessionHandler[%s]".formatted(connector.getIdentifier()));
        this.connector = connector;
        this.id = connector.getIdentifier();
        this.key = connector.getKey();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.state = ChannelState.INITIALIZED;
        logger.info("connected to server, logging in with id {}.", this.id);
        ctx.writeAndFlush(new P01_LoginRequest(this.id));
        this.state = ChannelState.PRE_LOGIN;
        ctx.fireChannelActive();

        logger.info("client session active.");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        this.connector.onLogout();
        ctx.fireChannelInactive();
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
        //active -  reject login packets(prevent attack)
        if (this.state == ChannelState.ACTIVE) {
            if (packet instanceof LoginPacket) {
                ctx.disconnect();
                return;
            }

            ctx.fireChannelRead(packet);
            return;
        }

        if (!(packet instanceof LoginPacket lp)) {
            ctx.disconnect();
            return;
        }

        if (lp instanceof P02_LoginResult lr) {
            if (!lr.getResult()) {
                logger.info("Login failed: {}", lr.getMessage());
                ctx.disconnect();
                return;
            }
        }

        if (this.state != lp.state()) {
            ctx.disconnect();
            return;
        }

        //init - send pre-login result
        //check if id exist
        if (this.state == ChannelState.PRE_LOGIN) {
            if (!(lp instanceof P02_LoginResult login)) {
                ctx.disconnect();
                return;
            }

            var ref = login.getVerificationRef();
            this.revVerifyRef = System.currentTimeMillis();
            ctx.writeAndFlush(new P03_Verification(this.revVerifyRef, APMProtocol.generateVerification(this.key, ref)));
            logger.info("passed key check, requesting rev-verification with ref {}.", this.revVerifyRef);

            this.state = ChannelState.POST_LOGIN;
            return;
        }

        if (this.state == ChannelState.POST_LOGIN) {
            if (!(lp instanceof P04_VerificationResult v)) {
                ctx.disconnect();
                return;
            }

            var data = v.getReverseVerification();
            var verification = APMProtocol.generateVerification(this.key, this.revVerifyRef);
            var members = v.getNetworkMembers();

            if (!Objects.equals(verification, data)) {
                logger.error("server failed reversed verification.");
                ctx.disconnect();
                return;
            }

            logger.info("server passed reversed verification, joined target network.");

            ctx.writeAndFlush(new P05_ConfirmLogin());
            this.state = ChannelState.ACTIVE;

            this.connector.getServerInGroup().clear();
            this.connector.getServerInGroup().addAll(members);
            this.connector.onLogin();
        }
    }
}
