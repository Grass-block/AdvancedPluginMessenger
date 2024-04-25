package me.gb2022.apm.local;

import me.gb2022.commons.container.MapBuilder;

import java.util.Map;
import java.util.function.Consumer;


public final class MappedBroadcastEvent extends MappedMessageEvent {
    public MappedBroadcastEvent(String id, Map<String, Object> properties) {
        super(id, properties);
    }

    public MappedBroadcastEvent(String id, Consumer<MapBuilder<String, Object>> defaults) {
        super(id, defaults);
    }
}
