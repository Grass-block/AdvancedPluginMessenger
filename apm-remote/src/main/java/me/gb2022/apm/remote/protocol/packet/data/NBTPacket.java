package me.gb2022.apm.remote.protocol.packet.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import me.gb2022.apm.remote.protocol.packet.DeserializedConstructor;
import me.gb2022.commons.nbt.NBT;
import me.gb2022.commons.nbt.NBTBase;

public final class NBTPacket extends DataPacket {
    private final NBTBase tag;
    private final boolean zipped;

    public NBTPacket(String channel, String receiver, NBTBase tag, boolean zipped) {
        super(channel, receiver);
        this.tag = tag;
        this.zipped = zipped;
    }

    @DeserializedConstructor
    public NBTPacket(ByteBuf data) {
        super(data);
        this.zipped = data.readBoolean();
        if (this.zipped) {
            this.tag = NBT.readZipped(new ByteBufInputStream(data));
        } else {
            this.tag = NBT.read(new ByteBufInputStream(data));
        }
    }


    @Override
    public void write0(ByteBuf data) {
        data.writeBoolean(this.zipped);
        if (this.zipped) {
            NBT.writeZipped(this.tag, new ByteBufOutputStream(data));
        } else {
            NBT.write(this.tag, new ByteBufOutputStream(data));
        }
    }

    public NBTBase getTag() {
        return tag;
    }
}
