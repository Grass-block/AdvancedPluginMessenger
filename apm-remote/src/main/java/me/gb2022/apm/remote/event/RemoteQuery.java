package me.gb2022.apm.remote.event;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.object.Server;

public class RemoteQuery extends RemoteMessage {
    private final ByteBuf result;

    public RemoteQuery(Server sender, ByteBuf data, ByteBuf result) {
        super(sender, data);
        this.result = result;
    }

    public ByteBuf getResult() {
        return result;
    }
}
