package me.gb2022.apm.remote.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public final class BlockableDaemon<V extends BlockedRunnable> implements Runnable {
    public static final Logger LOGGER = LogManager.getLogger("APM/TestDaemon");

    private final AtomicBoolean alive = new AtomicBoolean(true);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicReference<V> pointer;
    private final AtomicReference<ConnectorState> state;
    private final Lock pauseLock = new ReentrantLock();
    private final Lock activeLock = new ReentrantLock();
    private final Condition pauseCondition = this.pauseLock.newCondition();
    private final Condition activeCondition = this.activeLock.newCondition();
    private final Supplier<V> supplier;

    private boolean init = false;

    public BlockableDaemon(AtomicReference<V> pointer, AtomicReference<ConnectorState> state, Supplier<V> supplier) {
        this.pointer = pointer;
        this.state = state;
        this.supplier = supplier;
    }

    public void initialize() {
        this.init();
        this.init = true;
    }

    private void init() {
        this.pointer.set(this.supplier.get());
    }

    public void loop() throws InterruptedException {
        this.state.set(ConnectorState.OPENING);
        this.activeLock.lockInterruptibly();

        if (this.init) {
            this.init = false;
        } else {
            this.init();
        }

        try {
            LOGGER.info("restarted connector thread.");

            this.state.set(ConnectorState.OPENED);
            this.pointer.get().open();
        } catch (Throwable e) {
            LOGGER.error("an error occurred when running daemon:");
            LOGGER.catching(e);
        }

        this.state.set(ConnectorState.CLOSED);
        this.activeCondition.signalAll();
        this.activeLock.unlock();
    }


    private void close() {
        if (this.state.get() == ConnectorState.OPENED) {
            this.state.set(ConnectorState.CLOSING);
            this.pointer.get().close();
        }
    }

    public void quit() {
        this.alive.set(false);
        this.paused.set(false);
        this.close();
    }

    public CompletableFuture<Void> pause() {
        this.paused.set(true);
        this.close();

        return new CompletableFuture<>() {
            @Override
            public Void get() throws InterruptedException {
                waitForStop(Long.MAX_VALUE, TimeUnit.DAYS);
                return null;
            }

            @Override
            public Void get(long timeout, TimeUnit unit) throws InterruptedException {
                waitForStop(timeout, unit);
                return null;
            }
        };
    }

    public void resume() {
        this.pauseLock.lock();
        this.paused.set(false);
        if (this.state.get() == ConnectorState.CLOSED) {
            this.pauseCondition.signalAll();
        }
        this.pauseLock.unlock();
    }

    private void waitLock(int extraWait) throws InterruptedException {
        this.pauseLock.lockInterruptibly();

        var startTime = System.currentTimeMillis();

        try {
            while (this.paused.get()) {
                this.pauseCondition.await();
            }
        } finally {
            this.pauseLock.unlock();
        }

        var requiredWait = extraWait - (System.currentTimeMillis() - startTime);

        if (requiredWait <= 0) {
            return;
        }

        Thread.sleep(requiredWait);
    }

    public void waitForStop(long timeout, TimeUnit unit) throws InterruptedException {
        var nanos = unit.toNanos(timeout);
        this.activeLock.lock();

        try {
            while (true) {
                if (this.state.get() == ConnectorState.CLOSED) {
                    this.activeCondition.signalAll();
                    break;
                }

                if (nanos <= 0L) {
                    break;
                }

                nanos = this.activeCondition.awaitNanos(nanos);
            }
        } finally {
            this.activeLock.unlock();
        }
    }

    @Override
    public void run() {
        while (this.alive.get()) {
            try {
                loop();
                waitLock(5000);
            } catch (InterruptedException e) {
                LOGGER.error("received error on waiting lock:");
                LOGGER.catching(e);
            }
        }
    }
}
