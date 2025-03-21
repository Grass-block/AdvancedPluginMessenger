package me.gb2022.simpnet;

public class MessageVerifyFailedException extends RuntimeException{
    public MessageVerifyFailedException(String s) {
        super(s);
    }
}
