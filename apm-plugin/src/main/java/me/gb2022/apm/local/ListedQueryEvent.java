package me.gb2022.apm.local;

import me.gb2022.commons.container.ListBuilder;

import java.util.ArrayList;
import java.util.function.Consumer;


public final class ListedQueryEvent extends ListedMessageEvent {
    public ListedQueryEvent(String id) {
        super(id, new ArrayList<>(16));
    }

    public ListedQueryEvent(String id, Consumer<ListBuilder<Object>> defaults) {
        super(id, defaults);
    }

    public ListedQueryEvent setArgument(int position, Object arg) {
        this.getArgs().add(position, arg);
        return this;
    }

    public ListedQueryEvent addArgument(Object arg) {
        this.getArgs().add(arg);
        return this;
    }
}
