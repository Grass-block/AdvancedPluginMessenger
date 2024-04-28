package me.gb2022.apm.local;

import me.gb2022.commons.event.EventBus;

import java.lang.reflect.Method;
import java.util.List;

public class MessageEventBus extends EventBus<PluginMessageHandler, PluginMessageHandlerInstance> {
    @Override
    public PluginMessageHandlerInstance createHandlerInstance(Method method, PluginMessageHandler pluginMessageHandler, Object o) {
        return new PluginMessageHandlerInstance(o, method);
    }

    @Override
    public Class<PluginMessageHandler> getHandlerAnnotationClass() {
        return PluginMessageHandler.class;
    }

    @Override
    public void execute(List<PluginMessageHandlerInstance> list, Object o, Object[] o1) {
        for (PluginMessageHandlerInstance instance : list) {
            instance.call(o, o1);
        }
    }
}
