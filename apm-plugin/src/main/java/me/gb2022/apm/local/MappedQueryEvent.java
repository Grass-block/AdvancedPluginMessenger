package me.gb2022.apm.local;


import me.gb2022.commons.container.MapBuilder;

import java.util.HashMap;
import java.util.function.Consumer;


public final class MappedQueryEvent extends MappedMessageEvent {
    public MappedQueryEvent(String id) {
        super(id, new HashMap<>());
    }

    public MappedQueryEvent(String id, Consumer<MapBuilder<String, Object>> defaults) {
        super(id, defaults);
    }

    public MappedQueryEvent setProperty(String name, Object obj) {
        this.getProperties().put(name, obj);
        return this;
    }
}
