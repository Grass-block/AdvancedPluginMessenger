package me.gb2022.apm.remote.protocol.message;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.protocol.BufferUtil;

public class ServerLoginResult extends Message {
    private final boolean success;
    private final String[] objects;

    public ServerLoginResult(boolean success, String[] objects) {
        this.success = success;
        this.objects = objects;
    }

    public ServerLoginResult(ByteBuf data) {
        this.success = data.readBoolean();
        String s=BufferUtil.readString(data);
        this.objects = s.split(";");
    }

    @Override
    public void write(ByteBuf data) {
        data.writeBoolean(this.success);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.objects.length; i++) {
            sb.append(this.objects[i]);
            if (i != this.objects.length - 1) {
                sb.append(';');
            }
        }
        BufferUtil.writeString(data, sb.toString());
    }

    @Override
    public EnumMessages getType() {
        return EnumMessages.LOGIN_RESULT;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String[] getObjects() {
        return objects;
    }
}
