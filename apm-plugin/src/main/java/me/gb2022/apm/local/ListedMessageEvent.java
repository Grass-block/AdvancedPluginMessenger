package me.gb2022.apm.local;

import me.gb2022.commons.container.ListBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class ListedMessageEvent {
    private final String id;
    private final List<Object> args;

    public ListedMessageEvent(String id, List<Object> args) {
        this.id = id;
        this.args = args;
    }

    public ListedMessageEvent(String id, Consumer<ListBuilder<Object>> defaults) {
        this(id, init(defaults));
    }

    private static List<Object> init(Consumer<ListBuilder<Object>> consumer) {
        List<Object> args = new ArrayList<>(16);
        consumer.accept(new ListBuilder<>(args));
        return args;
    }

    public List<Object> getArgs() {
        return args;
    }

    public String getId() {
        return id;
    }

    public <T> T getArgument(int position, Class<T> type) {
        return type.cast(this.args.get(position));
    }
}
