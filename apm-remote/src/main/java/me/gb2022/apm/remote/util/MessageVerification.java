package me.gb2022.apm.remote.util;

import io.netty.buffer.ByteBuf;
import me.gb2022.commons.math.Crc16;
import me.gb2022.commons.math.NumberCodec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

public final class MessageVerification {
    private final Cipher encode;
    private final Cipher decode;

    public MessageVerification(Mode mode, byte[] key) {
        try {
            var md = MessageDigest.getInstance("MD5");
            md.update(key);
            var keySpec = new SecretKeySpec(md.digest(), "AES");

            this.encode = Cipher.getInstance(mode.toString());
            this.decode = Cipher.getInstance(mode.toString());
            this.encode.init(Cipher.ENCRYPT_MODE, keySpec);
            this.decode.init(Cipher.DECRYPT_MODE, keySpec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verify(byte[] sign, byte[] data) {
        try {
            var dataSign = Crc16.crc16(data);
            var dataDecoded = this.decode.doFinal(sign);

            if (dataDecoded.length < 4) {
                return false;
            }

            var decodedSign = NumberCodec.asInt(dataDecoded);

            return decodedSign == dataSign;
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            return false;
        }
    }

    public byte[] sign(byte[] data) {
        try {
            return this.encode.doFinal(NumberCodec.split(Crc16.crc16(data)));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean unpack(ByteBuf buffer, byte[] sign, byte[] data) {
        try {
            var decryptedData = this.decode.doFinal(data);

            if (!verify(sign, decryptedData)) {
                return false;
            }

            buffer.writeBytes(decryptedData);
            return true;
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            return false;
        }
    }

    public void pack(ByteBuf buffer) {
        buffer.readerIndex(0);
        var data = new byte[buffer.writerIndex()];
        buffer.readBytes(data);
        buffer.readerIndex(0);
        buffer.writerIndex(0);

        var sign = sign(data);
        byte[] encrypted;
        try {
            encrypted = this.encode.doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }

        buffer.writeByte(sign.length);
        buffer.writeInt(encrypted.length);

        buffer.writeBytes(sign);
        buffer.writeBytes(encrypted);
    }


    public enum Mode {
        AES_ECB("AES/ECB/PKCS5Padding"), AES_CBC("AES/CBC/PKCS5Padding"), AES_CFB("AES/CFB/PKCS5Padding");

        final String mode;

        Mode(String mode) {
            this.mode = mode;
        }

        @Override
        public String toString() {
            return mode;
        }
    }
}
