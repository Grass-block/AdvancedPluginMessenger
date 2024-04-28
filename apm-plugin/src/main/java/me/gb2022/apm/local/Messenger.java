package me.gb2022.apm.local;

import me.gb2022.commons.container.ListBuilder;
import me.gb2022.commons.container.MapBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface Messenger {
    String FETCH_KICK_MESSAGE = "quark:kick-message-fetch";

    MessageEventBus EVENT_BUS = new MessageEventBus();

    static void broadcastMapped(String id, Map<String, Object> properties) {
        EVENT_BUS.callEvent(new MappedBroadcastEvent(id, properties), id);
    }

    static void broadcastMapped(String id, Consumer<MapBuilder<String, Object>> propertiesInitializer) {
        EVENT_BUS.callEvent(new MappedBroadcastEvent(id, propertiesInitializer), id);
    }

    static void broadcastListed(String id, List<Object> properties) {
        EVENT_BUS.callEvent(new ListedBroadcastEvent(id, properties), id);
    }

    static void broadcastListed(String id, Consumer<ListBuilder<Object>> propertiesInitializer) {
        EVENT_BUS.callEvent(new ListedBroadcastEvent(id, propertiesInitializer), id);
    }

    static MappedQueryEvent queryMapped(String id, Consumer<MapBuilder<String, Object>> propertiesInitializer) {
        MappedQueryEvent event = new MappedQueryEvent(id, propertiesInitializer);
        EVENT_BUS.callEvent(event, id);
        return event;
    }

    static ListedQueryEvent queryListed(String id, Consumer<ListBuilder<Object>> propertiesInitializer) {
        ListedQueryEvent event = new ListedQueryEvent(id, propertiesInitializer);
        EVENT_BUS.callEvent(event, id);
        return event;
    }

    static String queryKickMessage(String playerName, String defaultMessage, String locale) {
        return Messenger.queryMapped(FETCH_KICK_MESSAGE, (map) -> {
            map.put("message", defaultMessage);
            map.put("player", playerName);
            map.put("locale", locale);
        }).getProperty("message", String.class);
    }
}
