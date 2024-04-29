package me.gb2022.apm.remote.protocol;

public enum MessageType {
    LOGIN(0),
    LOGIN_RESULT(1),
    LOGOUT(2),
    MESSAGE(3),
    QUERY(4),
    QUERY_RESULT(5);

    public final int id;

    MessageType(int id) {
        this.id = id;
    }

    public static MessageType of(byte b) {
        return switch (b) {
            case 0 -> LOGIN;
            case 1 -> LOGIN_RESULT;
            case 2 -> LOGOUT;
            case 3 -> MESSAGE;
            case 4 -> QUERY;
            case 5 -> QUERY_RESULT;
            default -> throw new IllegalStateException("Unexpected value: " + b);
        };
    }
}
