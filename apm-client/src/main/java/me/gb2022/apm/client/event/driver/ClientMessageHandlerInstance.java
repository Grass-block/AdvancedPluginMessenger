package me.gb2022.apm.client.event.driver;

import me.gb2022.commons.event.HandlerInstance;

import java.lang.reflect.Method;

public class ClientMessageHandlerInstance extends HandlerInstance {
    private final String channel;

    public ClientMessageHandlerInstance(Object handler, Method method) {
        super(handler, method);
        this.channel = method.getAnnotation(ClientEventHandler.class).value();
    }

    @Override
    public boolean shouldCall(Object o, Object... objects) {
        return objects[0].equals(this.channel);
    }
}
