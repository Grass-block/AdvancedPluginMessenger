package me.gb2022.simpnet.codec;

import io.netty.buffer.ByteBuf;
import me.gb2022.simpnet.util.BufferUtil;

import java.util.HashMap;
import java.util.Map;

public final class ObjectCodec {
    public static final Map<Class<?>, MessageDecoder<?>> DECODER = new HashMap<>();
    public static final Map<Class<?>, MessageEncoder<?>> ENCODER = new HashMap<>();

    static MessageCodec<Integer> INTEGER = new MessageCodec<>() {
        @Override
        public Integer decode(ByteBuf buffer) {
            return buffer.readInt();
        }

        @Override
        public void encode(Integer object, ByteBuf buffer) {
            buffer.writeInt(object);
        }
    };
    static MessageCodec<Long> LONG = new MessageCodec<>() {
        @Override
        public Long decode(ByteBuf buffer) {
            return buffer.readLong();
        }

        @Override
        public void encode(Long object, ByteBuf buffer) {
            buffer.writeLong(object);
        }
    };
    static MessageCodec<Float> FLOAT = new MessageCodec<>() {

        @Override
        public void encode(Float object, ByteBuf buffer) {
            buffer.writeFloat(object);
        }

        @Override
        public Float decode(ByteBuf buffer) {
            return buffer.readFloat();
        }
    };
    static MessageCodec<Double> DOUBLE = new MessageCodec<>() {
        @Override
        public Double decode(ByteBuf buffer) {
            return buffer.readDouble();
        }

        @Override
        public void encode(Double object, ByteBuf buffer) {
            buffer.writeDouble(object);
        }
    };
    static MessageCodec<Boolean> BOOLEAN = new MessageCodec<>() {
        @Override
        public Boolean decode(ByteBuf buffer) {
            return buffer.readBoolean();
        }

        @Override
        public void encode(Boolean object, ByteBuf buffer) {
            buffer.writeBoolean(object);
        }
    };
    static MessageCodec<String> STRING = new MessageCodec<>() {
        @Override
        public void encode(String object, ByteBuf buffer) {
            BufferUtil.writeString(buffer, object);
        }

        @Override
        public String decode(ByteBuf buffer) {
            return BufferUtil.readString(buffer);
        }
    };
    static MessageCodec<byte[]> BYTE_ARRAY = new MessageCodec<>() {
        @Override
        public byte[] decode(ByteBuf buffer) {
            var len = buffer.readInt();
            var array = new byte[len];
            buffer.readBytes(array);
            return array;
        }

        @Override
        public void encode(byte[] object, ByteBuf buffer) {
            buffer.writeInt(object.length);
            buffer.writeBytes(object);
        }
    };


    static {
        registerCodec(Integer.class, INTEGER);
        registerCodec(Long.class, LONG);
        registerCodec(Float.class, FLOAT);
        registerCodec(Double.class, DOUBLE);
        registerCodec(Boolean.class, BOOLEAN);
        registerCodec(String.class, STRING);
        registerCodec(byte[].class, BYTE_ARRAY);
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
        if (ByteBuf.class.isAssignableFrom(type)) {
            return (I) message;
        }

        return getDecoder(type).decode(message);
    }

    @SuppressWarnings("unchecked") //user checks
    public static <I> void encode(ByteBuf msg, I obj) {
        if (obj instanceof ByteBuf) {
            return;
        }

        getEncoder(((Class<I>) obj.getClass())).encode(obj, msg);
    }
}
