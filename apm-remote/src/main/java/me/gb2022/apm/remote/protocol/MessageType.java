package me.gb2022.apm.remote.protocol;

public enum MessageType {
    MESSAGE(0x0),
    QUERY(0x1),
    BROADCAST(0x2);

    final int id;

    MessageType(int id) {
        this.id = id;
    }

    public byte getId() {
        return (byte) id;
    }
}
