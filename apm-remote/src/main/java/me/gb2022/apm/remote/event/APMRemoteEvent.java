package me.gb2022.apm.remote.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface APMRemoteEvent {
    String GLOBAL = "__global__";

    String value() default GLOBAL; //channel
}
