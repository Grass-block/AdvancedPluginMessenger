package me.gb2022.apm.remote.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteEventHandler {
    String LISTENER_GLOBAL_EVENT_CHANNEL="__global__";

    String value() default LISTENER_GLOBAL_EVENT_CHANNEL; //channel
}
