package me.gb2022.apm.remote.codec;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.util.BufferUtil;

import java.util.HashMap;
import java.util.Map;

public final class ObjectCodec {
    public static final Map<Class<?>, MessageDecoder<?>> DECODER = new HashMap<>();
    public static final Map<Class<?>, MessageEncoder<?>> ENCODER = new HashMap<>();

    static {
        registerCodec(int.class, new MessageCodec<Integer>() {
            @Override
            public Integer decode(ByteBuf buffer) {
                return buffer.readInt();
            }

            @Override
            public void encode(Integer object, ByteBuf buffer) {
                buffer.writeInt(object);
            }
        });
        registerCodec(String.class, new MessageCodec<String>() {

            @Override
            public void encode(String object, ByteBuf buffer) {
                BufferUtil.writeString(buffer, object);
            }

            @Override
            public String decode(ByteBuf buffer) {
                return BufferUtil.readString(buffer);
            }
        });
    }

    public static void registerEncoder(Class<?> clazz, MessageEncoder<?> encoder) {
        ENCODER.put(clazz, encoder);
    }

    public static void registerDecoder(Class<?> clazz, MessageDecoder<?> decoder) {
        DECODER.put(clazz, decoder);
    }

    public static void registerCodec(Class<?> clazz, MessageCodec<?> codec) {
        registerEncoder(clazz, codec);
        registerDecoder(clazz, codec);
    }

    @SuppressWarnings("unchecked") //user checks
    public static <I> MessageDecoder<I> getDecoder(Class<I> clazz) {
        return (MessageDecoder<I>) DECODER.get(clazz);
    }

    @SuppressWarnings("unchecked") //user checks
    public static <I> MessageEncoder<I> getEncoder(Class<I> clazz) {
        return (MessageEncoder<I>) ENCODER.get(clazz);
    }

    public static <I> I decode(ByteBuf message, Class<I> type) {
        if(ByteBuf.class.isAssignableFrom(type)) {
            return (I) message;
        }

        return getDecoder(type).decode(message);
    }

    @SuppressWarnings("unchecked") //user checks
    public static <I> void encode(ByteBuf msg, I obj) {
        if(obj instanceof ByteBuf) {
            return;
        }

        getEncoder(((Class<I>) obj.getClass())).encode(obj, msg);
    }
}
