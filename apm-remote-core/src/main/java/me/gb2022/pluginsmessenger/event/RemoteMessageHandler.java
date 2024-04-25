package me.gb2022.pluginsmessenger.event;

import me.gb2022.pluginsmessenger.protocol.MessageType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteMessageHandler {
    MessageType type() default MessageType.MESSAGE;

    String channel();
}
