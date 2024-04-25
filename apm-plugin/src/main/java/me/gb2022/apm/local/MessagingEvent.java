package me.gb2022.apm.local;

import java.util.Objects;

public abstract class MessagingEvent {
    private final String id;

    protected MessagingEvent(String id) {
        this.id = id;
    }

    public boolean isRequestedEvent(String id) {
        return Objects.equals(id, this.id);
    }

    public String getId() {
        return id;
    }
}
