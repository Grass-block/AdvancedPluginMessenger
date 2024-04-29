package me.gb2022.apm.remote;

import me.gb2022.commons.container.ObjectContainer;

import java.util.function.Function;
import java.util.logging.Logger;

public interface APMLoggerManager {
    ObjectContainer<Function<String, Logger>> LOGGER_CREATOR = new ObjectContainer<>();

    static void setLoggerCreator(Function<String, Logger> loggerCreator) {
        LOGGER_CREATOR.set(loggerCreator);
    }

    static void useDebugLogger() {
        setLoggerCreator(SOUTLogger::new);
    }

    static Logger createLogger(String s) {
        return LOGGER_CREATOR.get().apply(s);
    }
}
