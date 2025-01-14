package me.gb2022.apm.client;

public interface ClientMessageProtocol {
    String PROTOCOL_PREFIX = "${";

    String MESSAGE_FORMAT = "${client:%s}%s";

    String CLIENT_RESPONSE_PROTOCOL = "${client:response}{\"path\":\"%s\",\"data\":%s}";


    String CLIENT_PROTOCOL_DETECT = "${client:detect}";
    String CLIENT_PROTOCOL_INIT = "${client:init}";
}
