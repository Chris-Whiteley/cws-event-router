package com.cwsoft.eventrouter;

import java.lang.annotation.*;

@Repeatable(Handlers.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)

/*
 * Used to annotate methods that handle events published through the EventDispatcher instance.
 */
public @interface EventHandler {
    /**
     * indicates the names of the event(s) that this method is subscribing to.
     * @return the names of the events.
     */
    String[] name();

    /**
     * indicates if the event(s) this handler is subscribing to are global events e.g. originate from another server/service.
     *
     * @return false if the event(s) are local to this server/service, true if the event(s) originate from another server/service.
     */
    Access[] access() default {Access.LOCAL};
}

