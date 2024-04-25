package me.gb2022.apm.local;

import me.gb2022.commons.container.ListBuilder;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class ListedBroadcastEvent extends ListedMessageEvent {
    public ListedBroadcastEvent(String id, Object... arguments) {
        super(id, List.of(arguments));
    }

    public ListedBroadcastEvent(String id, List<Object> arguments) {
        super(id, arguments);
    }

    public ListedBroadcastEvent(String id, Set<Object> arguments) {
        super(id, arguments.stream().toList());
    }

    public ListedBroadcastEvent(String id, Consumer<ListBuilder<Object>> defaults) {
        super(id, defaults);
    }
}
