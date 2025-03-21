package me.gb2022.simpnet.packet;

public class InvalidPacketFormatException extends RuntimeException{
    public InvalidPacketFormatException(Exception e) {
        super(e);
    }
}
