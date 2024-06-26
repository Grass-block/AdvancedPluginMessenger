package me.gb2022.apm.remote;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class SOUTLogger extends Logger {
    public SOUTLogger(String name) {
        super(name, null);
    }

    @Override
    public void log(LogRecord record) {
        System.out.printf(
                "%s [%s] %s: %s%n",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(record.getMillis())),
                record.getLevel().getName(),
                record.getLoggerName(),
                record.getMessage()
        );
    }
}
