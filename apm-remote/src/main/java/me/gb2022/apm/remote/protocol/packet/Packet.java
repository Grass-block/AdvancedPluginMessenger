package me.gb2022.apm.remote.protocol.packet;

import io.netty.buffer.ByteBuf;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Packet {

    void write(ByteBuf data);

    final class Registry {
        public static final Registry REGISTRY = new Registry();
        private final List<Class<? extends Packet>> i2c = new ArrayList<>();
        private final Map<Class<? extends Packet>, Integer> c2i = new HashMap<>();

        {
            for (var i = 0; i < 128; i++) {
                this.i2c.add(null);
            }

            register(0x00, P_Login.class);
            register(0x01, P_LoginResult.class);
            register(0x02, P_Logout.class);
            register(0x03, P_ServerLogin.class);
            register(0x04, P_ServerLogout.class);

            register(0x10, D_Raw.class);
        }

        public void register(int id, Class<? extends Packet> packet) {
            this.i2c.set(id, packet);
            this.c2i.put(packet, id);
        }

        public void encode(Packet packet, ByteBuf data) {
            data.resetReaderIndex();
            data.resetWriterIndex();

            var id = this.c2i.get(packet.getClass());

            data.writeByte(id);
            packet.write(data);
        }

        public Packet decode(ByteBuf data) {
            var id = data.readByte();
            var clazz = this.i2c.get(id);

            if (clazz == null) {
                throw new RuntimeException("Unknown packet id: " + id);
            }

            try {
                return clazz.getDeclaredConstructor(ByteBuf.class).newInstance(data);
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
