package me.gb2022.apm.client.event.driver;

import me.gb2022.commons.event.EventBus;
import me.gb2022.commons.event.HandlerInstance;

import java.lang.reflect.Method;
import java.util.List;

public class ClientMessageEventBus extends EventBus<ClientEventHandler, ClientMessageEventBus.RemoteMessageHandlerInstance> {

    @Override
    public RemoteMessageHandlerInstance createHandlerInstance(Method method, ClientEventHandler remoteMessageHandler, Object o) {
        return new RemoteMessageHandlerInstance(o, method);
    }

    @Override
    public Class<ClientEventHandler> getHandlerAnnotationClass() {
        return ClientEventHandler.class;
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
            this.channel = method.getAnnotation(ClientEventHandler.class).value();
        }

        @Override
        public boolean shouldCall(Object o, Object[] objects) {
            return objects[0].equals(this.channel);
        }
    }
}
