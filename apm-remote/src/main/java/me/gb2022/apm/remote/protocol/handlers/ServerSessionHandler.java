package me.gb2022.apm.remote.protocol.handlers;

import io.netty.channel.ChannelHandlerContext;
import me.gb2022.apm.remote.connector.ExchangeConnector;
import me.gb2022.apm.remote.protocol.APMProtocol;
import me.gb2022.apm.remote.protocol.ChannelState;
import me.gb2022.apm.remote.protocol.packet.*;
import me.gb2022.simpnet.packet.Packet;
import me.gb2022.simpnet.packet.PacketInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

public final class ServerSessionHandler extends PacketInboundHandler {
    public static final String LOG_DC_ILLEGAL_PACKET = "unexpected login from {}: illegal packets on state {}.";
    public static final String LOG_DC_ILLEGAL_IDENTITY = "unexpected login from {}: tried to join with existing id: {}.";
    public static final String LOG_DC_ILLEGAL_KEY = "unexpected login from {}: tried to join with illegal key.";
    public static final String KEY_DC_ILLEGAL_PACKET = "Illegal packet in protocol definition";

    public final Logger logger;
    private final byte[] key;
    private final ExchangeConnector connector;
    private String endpointId;
    private long loginVerificationRef;
    private ChannelState state;

    public ServerSessionHandler(ExchangeConnector connector) {
        this.logger = LogManager.getLogger("APM/SVSessionHandler[%s]".formatted(connector.getIdentifier()));
        this.connector = connector;
        this.key = connector.getKey();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.state = ChannelState.INITIALIZED;
        ctx.fireChannelActive();

        logger.info("server session active.");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (this.endpointId != null) {
            this.connector.onServerLeft(this.endpointId);
        }

        this.state = ChannelState.FAILED;
        this.endpointId = null;
        ctx.fireChannelInactive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {

        //active -  reject login packets(prevent attack)
        if (this.state == ChannelState.ACTIVE) {
            if (packet instanceof LoginPacket) {
                this.disconnectLogin(ctx, LOG_DC_ILLEGAL_PACKET, KEY_DC_ILLEGAL_PACKET, this.state);
                return;
            }

            ctx.fireChannelRead(packet);
            return;
        }

        if (!(packet instanceof LoginPacket lp)) {
            this.disconnectLogin(ctx, LOG_DC_ILLEGAL_PACKET, KEY_DC_ILLEGAL_PACKET, this.state);
            return;
        }

        if (this.state != lp.state()) {
            this.disconnectLogin(ctx, LOG_DC_ILLEGAL_PACKET, KEY_DC_ILLEGAL_PACKET, this.state);
            return;
        }

        //init - send pre-login result
        //check if id exist
        if (this.state == ChannelState.INITIALIZED) {
            var login = ((P01_LoginRequest) lp);
            if (this.connector.getServerInGroup().contains(login.getId())) {
                this.disconnectLogin(ctx, LOG_DC_ILLEGAL_IDENTITY, "Illegal/Existing identity", login.getId());
                return;
            }

            this.endpointId = login.getId();
            this.loginVerificationRef = System.currentTimeMillis();

            logger.info("passed identity check, starting key verification with ref {}.", this.endpointId, this.loginVerificationRef);
            ctx.writeAndFlush(new P02_LoginResult(true, "", this.loginVerificationRef));

            this.state = ChannelState.PRE_LOGIN;
            return;
        }

        if (this.state == ChannelState.PRE_LOGIN) {
            var v = ((P03_Verification) lp);

            var data = v.getVerification();
            var verification = APMProtocol.generateVerification(this.key, this.loginVerificationRef);

            if (!Objects.equals(verification, data)) {
                logger.error("{} has failed key verification.", this.endpointId);
                this.disconnectLogin(ctx, LOG_DC_ILLEGAL_KEY, "Illegal key verification", this.state);
                return;
            }

            logger.info("passed key check, sending reversed verification with ref {}.", this.endpointId, v.getReverseVerificationRef());
            var rev = APMProtocol.generateVerification(this.key, v.getReverseVerificationRef());

            ctx.writeAndFlush(new P04_VerificationResult(rev, this.connector.getServerInGroup()));
            this.state = ChannelState.POST_LOGIN;
            return;
        }

        if (this.state == ChannelState.POST_LOGIN) {
            this.connector.onServerJoin(this.endpointId, ctx.channel());
            this.state = ChannelState.ACTIVE;
        }
    }


    public String getEndpointId() {
        return this.endpointId;
    }

    private void disconnectLogin(ChannelHandlerContext ctx, String reason, String disconnectId, Object param) {
        ctx.writeAndFlush(new P02_LoginResult(false, disconnectId, -1L));
        ctx.disconnect();
        logger.error(reason, ctx.channel().remoteAddress(), param);
        logger.error("disconnected {}, connection ended.", ctx.channel().remoteAddress());
    }
}
