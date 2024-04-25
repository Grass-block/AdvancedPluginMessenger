package me.gb2022.apm.local;

import me.gb2022.commons.event.HandlerInstance;

import java.lang.reflect.Method;

public class PluginMessageHandlerInstance extends HandlerInstance {
    private final String channel;

    public PluginMessageHandlerInstance(Object handler, Method method) {
        super(handler, method);
        this.channel = method.getAnnotation(PluginMessageHandler.class).value();
    }

    @Override
    public boolean shouldCall(Object o, Object... objects) {
        return objects[0].equals(this.channel);
    }
}
