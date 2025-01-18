package me.gb2022.apm.remote.event;

import me.gb2022.apm.remote.RemoteMessenger;
import me.gb2022.apm.remote.event.connector.ConnectorReadyEvent;
import me.gb2022.apm.remote.event.connector.ConnectorStartEvent;
import me.gb2022.apm.remote.event.connector.EndpointLoginResultEvent;
import me.gb2022.apm.remote.event.message.RemoteMessageEvent;
import me.gb2022.apm.remote.event.message.RemoteMessageSurpassEvent;
import me.gb2022.apm.remote.event.message.RemoteQueryEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public interface RemoteEventListener {
    default void endpointLogin(RemoteMessenger messenger, ConnectorStartEvent event) {
    }

    default void endpointLoginResult(RemoteMessenger messenger, EndpointLoginResultEvent event) {
    }

    default void connectorReady(RemoteMessenger messenger, ConnectorReadyEvent event) {
    }

    default void remoteMessage(RemoteMessenger messenger, RemoteMessageEvent event) {
    }

    default void remoteQuery(RemoteMessenger messenger, RemoteQueryEvent event) {
    }

    default void messageSurpassed(RemoteMessenger messenger, RemoteMessageSurpassEvent event) {
    }

    default void endpointJoined(RemoteMessenger messenger, EndpointJoinEvent event) {
    }

    default void endpointLeft(RemoteMessenger messenger, EndpointLeftEvent event) {
    }

    abstract class EventHolder<C, E> {
        private final Class<C> contextClass;
        private final Class<E> eventClass;

        private final Map<Class<?>, Map<String, Method>> executors;
        private final Object invoker;

        public EventHolder(Class<C> contextClass, Class<E> eventClass, Object invoker) {
            this.contextClass = contextClass;
            this.eventClass = eventClass;
            this.invoker = invoker;
            this.executors = bakeEventExecutors(invoker.getClass());
        }

        public EventHolder(Class<C> contextClass, Class<E> eventClass, Class<?> template) {
            this.contextClass = contextClass;
            this.eventClass = eventClass;
            this.invoker = null;
            this.executors = bakeEventExecutors(template);
        }

        private Map<Class<?>, Map<String, Method>> bakeEventExecutors(Class<?> c) {
            var map = new HashMap<Class<?>, Map<String, Method>>();

            for (var m : c.getMethods()) {
                m.setAccessible(true);

                var channel = this.processEvent(m);

                if (channel == null) {
                    continue;
                }

                if (!Modifier.isStatic(m.getModifiers()) && this.invoker == null) {
                    continue;
                }

                var args = m.getParameters();

                if (args.length != 2) {
                    throw new RuntimeException("Invalid remote event handler injection: " + m);
                }

                var eventType = args[1].getType();

                if (args[0].getType() != this.contextClass || !this.eventClass.isAssignableFrom(eventType)) {
                    throw new RuntimeException("Invalid remote event handler injection: " + m);
                }


                var slot = map.computeIfAbsent(eventType, (k) -> new HashMap<>());

                slot.put(channel, m);
            }

            return map;
        }

        public void dispatchEvent(C context, E event, String channel) {
            var eventType = event.getClass();

            if (!this.executors.containsKey(eventType)) {
                return;
            }

            var slot = this.executors.get(eventType);

            if (!slot.containsKey(channel)) {
                return;
            }

            try {
                slot.get(channel).invoke(this.invoker, context, event);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public abstract String processEvent(Method method);
    }

    final class APAdapter extends EventHolder<RemoteMessenger, RemoteEvent> implements RemoteEventListener {
        public static final Map<Object, APAdapter> INSTANCES = new HashMap<>();

        private APAdapter(Object handle) {
            super(RemoteMessenger.class, RemoteEvent.class, handle);
        }

        private APAdapter(Class<?> handle) {
            super(RemoteMessenger.class, RemoteEvent.class, handle);
        }

        public static APAdapter getInstance(Object handle) {
            return INSTANCES.computeIfAbsent(handle, APAdapter::new);
        }

        public static APAdapter getInstance(Class<?> handle) {
            return INSTANCES.computeIfAbsent(handle, (k) -> new APAdapter(handle));
        }

        public static void clearInstance(Class<?> handle) {
            INSTANCES.remove(handle);
        }

        public static void clearInstance(Object handle) {
            INSTANCES.remove(handle);
        }


        @Override
        public void endpointLogin(RemoteMessenger messenger, ConnectorStartEvent event) {
            dispatchEvent(messenger, event, APMRemoteEvent.GLOBAL);
        }

        @Override
        public void endpointLoginResult(RemoteMessenger messenger, EndpointLoginResultEvent event) {
            dispatchEvent(messenger, event, APMRemoteEvent.GLOBAL);
        }

        @Override
        public void connectorReady(RemoteMessenger messenger, ConnectorReadyEvent event) {
            dispatchEvent(messenger, event, APMRemoteEvent.GLOBAL);
        }

        @Override
        public void remoteMessage(RemoteMessenger messenger, RemoteMessageEvent event) {
            dispatchEvent(messenger, event, event.channel());
        }

        @Override
        public void remoteQuery(RemoteMessenger messenger, RemoteQueryEvent event) {
            dispatchEvent(messenger, event, event.channel());
        }

        @Override
        public void messageSurpassed(RemoteMessenger messenger, RemoteMessageSurpassEvent event) {
            dispatchEvent(messenger, event, event.channel());
        }

        @Override
        public void endpointJoined(RemoteMessenger messenger, EndpointJoinEvent event) {
            dispatchEvent(messenger, event, APMRemoteEvent.GLOBAL);
        }

        @Override
        public void endpointLeft(RemoteMessenger messenger, EndpointLeftEvent event) {
            dispatchEvent(messenger, event, APMRemoteEvent.GLOBAL);
        }

        @Override
        public String processEvent(Method method) {
            if (!method.isAnnotationPresent(APMRemoteEvent.class)) {
                return null;
            }

            return method.getAnnotation(APMRemoteEvent.class).value();
        }
    }
}
