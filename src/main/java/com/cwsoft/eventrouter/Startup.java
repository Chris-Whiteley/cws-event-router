package com.cwsoft.eventrouter;

import lombok.extern.slf4j.Slf4j;

/**
 * Class to startup Event Dispatching framework.
 */

@Slf4j
public abstract class Startup {

    public EventHandlers handlers;
    public BeanSupplier beanSupplier;

    /**
     * this should be called once the application context (BeanSupplier) has been initialised with all beans in the
     * service (application).  It sets up all the handlers based on beans that have methods annotated with @EventHandler.
     * Once initialisation is complete it dispatches the event "RebasoftContextReady" as certain beans may want to be made aware of this.
     */
    public void start() {
        if (handlers == null) throw new NullPointerException("Required field handlers has not been set");
        EventDispatcher  eventDispatcher = EventDispatcher.instanceOf;
        if (eventDispatcher == null) throw new IllegalStateException("Event Dispatcher has not been created");
        if (beanSupplier == null) throw new NullPointerException("Required field beanSupplier has not been set");
        handlers.init(beanSupplier);
        // rebasoft-context (RebasoftApplicationContext and Event Dispatcher) now ready!
        eventDispatcher.dispatchEvent(new NamedEvent("RebasoftContextReady"));
    }
}
