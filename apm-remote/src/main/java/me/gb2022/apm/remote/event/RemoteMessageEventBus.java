package me.gb2022.apm.remote.event;

import me.gb2022.commons.event.EventBus;
import me.gb2022.commons.event.HandlerInstance;

import java.lang.reflect.Method;
import java.util.List;

public class RemoteMessageEventBus extends EventBus<RemoteEventHandler, RemoteMessageEventBus.RemoteMessageHandlerInstance> {

    @Override
    public RemoteMessageHandlerInstance createHandlerInstance(Method method, RemoteEventHandler remoteMessageHandler, Object o) {
        return new RemoteMessageHandlerInstance(o, method);
    }

    @Override
    public Class<RemoteEventHandler> getHandlerAnnotationClass() {
        return RemoteEventHandler.class;
    }

    @Override
    public void execute(List<RemoteMessageHandlerInstance> list, Object o, Object[] objects) {
        for (RemoteMessageHandlerInstance instance : list) {
            instance.call(o, objects);
        }
    }

    public static final class RemoteMessageHandlerInstance extends HandlerInstance {
        private final String channel;

        public RemoteMessageHandlerInstance(Object handler, Method method) {
            super(handler, method);
            this.channel = method.getAnnotation(RemoteEventHandler.class).value();
        }

        @Override
        public boolean shouldCall(Object o, Object[] objects) {
            return objects[0].equals(this.channel);
        }
    }
}
