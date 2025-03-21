package me.gb2022.apm.remote.event.legacy;

import me.gb2022.apm.remote.event.APMRemoteEvent;
import me.gb2022.commons.event.EventBus;
import me.gb2022.commons.event.HandlerInstance;

import java.lang.reflect.Method;
import java.util.List;

public class RemoteMessageEventBus extends EventBus<APMRemoteEvent, RemoteMessageEventBus.RemoteMessageHandlerInstance> {

    @Override
    public RemoteMessageHandlerInstance createHandlerInstance(Method method, APMRemoteEvent remoteMessageHandler, Object o) {
        return new RemoteMessageHandlerInstance(o, method);
    }

    @Override
    public Class<APMRemoteEvent> getHandlerAnnotationClass() {
        return APMRemoteEvent.class;
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
            this.channel = method.getAnnotation(APMRemoteEvent.class).value();
        }

        @Override
        public boolean shouldCall(Object o, Object[] objects) {
            return objects[0].equals(this.channel);
        }
    }
}
