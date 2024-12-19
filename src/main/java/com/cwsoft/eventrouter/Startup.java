package com.cwsoft.eventrouter;

import lombok.extern.slf4j.Slf4j;

/**
 * Class to startup Event Dispatching framework.
 */

@Slf4j
public abstract class Startup {

    private final EventHandlers handlers;
    private final BeanSupplier beanSupplier;

    public Startup(EventHandlers handlers, BeanSupplier beanSupplier) {
        if (handlers == null || beanSupplier == null) {
            throw new IllegalArgumentException("handlers and beanSupplier must not be null");
        }
        this.handlers = handlers;
        this.beanSupplier = beanSupplier;
    }

   /*
    * this should be called once the application context (BeanSupplier) has been initialised with all beans in the
    * service (application).  It sets up all the handlers based on beans that have methods annotated with @EventHandler.
    * Once initialisation is complete it dispatches the event todo: change name "RebasoftContextReady" as certain beans may want to be made aware of this.
    */
    public void start() {
        log.info("Starting EventRouter initialization...");
        EventDispatcher eventDispatcher = EventDispatcher.instanceOf;

        if (eventDispatcher == null) {
            throw new IllegalStateException("EventDispatcher has not been created");
        }

        handlers.init(beanSupplier);
        log.info("EventRouter initialized successfully.");

        NamedEvent contextReadyEvent = new NamedEvent("RebasoftContextReady");
        eventDispatcher.dispatchEvent(contextReadyEvent);
        log.info("Dispatched event: {}", contextReadyEvent.getName());
    }
}
