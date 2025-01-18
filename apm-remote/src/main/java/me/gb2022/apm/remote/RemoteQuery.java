package me.gb2022.apm.remote;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import me.gb2022.apm.remote.codec.ObjectCodec;
import me.gb2022.apm.remote.event.RemoteEventListener;
import me.gb2022.apm.remote.event.message.RemoteMessageEvent;
import me.gb2022.commons.container.Pair;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class RemoteQuery<I> {
    public static final ByteBuf EMPTY_RESULT_POINTER = ByteBufAllocator.DEFAULT.buffer(1, 1);
    public static final ScheduledExecutorService TIMER_EXECUTOR_POOL = Executors.newSingleThreadScheduledExecutor();

    private final String uuid;
    private final BlockingQueue<ByteBuf> sync = new ArrayBlockingQueue<>(1);
    private final Consumer<String> send;
    private final Class<I> type;
    private Pair<Long, Runnable> timeout;
    private Consumer<I> result;
    private Consumer<Throwable> error;

    public RemoteQuery(String uuid, Class<I> type, Consumer<String> send) {
        this.uuid = uuid;
        this.send = send;
        this.type = type;
    }

    public static <D> RemoteQuery<D> of(RemoteMessenger messenger, Class<D> type, Consumer<String> senderAction) {
        var uuid = UUID.randomUUID().toString();
        var query = new RemoteQuery<>(uuid, type, senderAction);

        messenger.queryHolder().register(uuid, query);

        return query;
    }

    public void request() {
        this.send.accept(this.uuid);

        if (this.timeout != null) {
            TIMER_EXECUTOR_POOL.execute(() -> {
                try {
                    Thread.sleep(this.timeout.getLeft());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    this.sync.add(EMPTY_RESULT_POINTER);
                }
                this.sync.add(EMPTY_RESULT_POINTER);
            });
        }

        try {
            var object = this.sync.take();

            if (object == EMPTY_RESULT_POINTER && this.timeout != null) {
                this.timeout.getRight().run();
                return;
            }

            if (this.result == null) {
                error(new NullPointerException("need a result handler!"));
            }

            this.result.accept(ObjectCodec.decode(object, this.type));

            object.release();
        } catch (Throwable e) {
            error(e);
        }
    }

    public RemoteQuery<I> error(Consumer<Throwable> error) {
        this.error = error;
        return this;
    }

    public RemoteQuery<I> result(Consumer<I> result) {
        this.result = result;
        return this;
    }

    private void error(Throwable e) {
        if (this.error != null) {
            this.error.accept(e);
        } else {
            e.printStackTrace();
        }
    }

    public RemoteQuery<I> timeout(long mills, Runnable command) {
        this.timeout = new Pair<>(mills, command);
        return this;
    }

    public static final class Holder implements RemoteEventListener {
        private final ConcurrentHashMap<String, RemoteQuery<?>> lookups = new ConcurrentHashMap<>();

        public void receive(String pid, ByteBuf message) {
            var it = this.lookups.entrySet().iterator();

            while (it.hasNext()) {
                var entry = it.next();
                var id = entry.getKey();
                var handler = entry.getValue();

                if (!Objects.equals(id, pid)) {
                    continue;
                }

                handler.sync.add(message.copy());
                it.remove();
                return;
            }
        }

        public void clear() {
            for (var entry : this.lookups.entrySet()) {
                entry.getValue().sync.add(EMPTY_RESULT_POINTER);
            }
        }

        @Override
        public void remoteMessage(RemoteMessenger messenger, RemoteMessageEvent event) {
            receive(event.uuid(), event.message());
        }

        public void register(String uuid, RemoteQuery<?> query) {
            this.lookups.put(uuid, query);
        }
    }
}