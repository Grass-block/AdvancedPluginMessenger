package me.gb2022.simpnet.packet;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class PacketRegistry {
    public static final PacketRegistry REGISTRY = new PacketRegistry();
    private final List<Class<? extends Packet>> i2c = new ArrayList<>();
    private final Map<Class<? extends Packet>, Integer> c2i = new HashMap<>();

    public PacketRegistry() {
        super();
    }

    public PacketRegistry(int cap, Consumer<PacketRegistry> initializer) {
        this.allocate(cap);
        initializer.accept(this);
    }

    public void allocate(int cap) {
        for (var i = 0; i < cap; i++) {
            this.i2c.add(null);
        }
    }

    public void register(int id, Class<? extends Packet> packet) {
        this.i2c.set(id, packet);
        this.c2i.put(packet, id);
    }

    public void encode(Packet packet, ByteBuf data) {
        try {
            data.resetReaderIndex();
            data.resetWriterIndex();

            var id = this.c2i.get(packet.getClass());

            data.writeByte(id);
            packet.write(data);
        } catch (Exception e) {
            throw new InvalidPacketFormatException(e);
        }
    }

    public Packet decode(ByteBuf data) {
        try {
            var id = data.readByte();
            var clazz = this.i2c.get(id);

            if (clazz == null) {
                throw new RuntimeException("Unknown packet id: " + id);
            }

            return clazz.getDeclaredConstructor(ByteBuf.class).newInstance(data);
        } catch (Exception e) {
            throw new InvalidPacketFormatException(e);
        }
    }
}
