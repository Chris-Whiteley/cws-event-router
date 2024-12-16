package com.cwsoft.eventrouter.global;

import com.cwsoft.eventrouter.Event;
import com.cwsoft.eventrouter.EventDispatcher;
import com.cwsoft.eventrouter.EventHandler;
import com.cwsoft.messaging.ClosableConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Consumes global event messages for events that occurred in remote services and dispatches them
 * locally to the handler which will have been annotated with @EventHandler(name = "someName", access = Access.GLOBAL).
 * The topic consumed from is named "events.dispatch.events_for_service_" + forServiceId.replace(" ", "_");
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class GlobalEventsConsumer {

    private final ClosableConsumer<Event> consumer;
    private final EventDispatcher eventDispatcher;

    private final Duration consumeTimeout = Duration.ofSeconds(5); // Configurable timeout
    private final int initialBackoff = 1000; // Configurable backoff (in ms)

    @EventHandler(name = "onStartup")
    public void init() {
        startConsumerThread();
    }

    private void startConsumerThread() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                consumer.close();
            } catch (Exception e) {
                log.warn("Error while closing consumer", e);
            }
            executorService.shutdownNow();
        }));
        executorService.submit(this::runConsumer);
    }

    private void runConsumer() {
        Thread.currentThread().setName("G-EVT-CONSUMER");

        log.info("runConsumer started");
        final int[] backoff = {initialBackoff};
        final int maxBackoff = 16000; // Cap at 16 seconds

        while (!Thread.currentThread().isInterrupted()) {
            try {
                consumer.consume(consumeTimeout)
                        .ifPresent(event -> {
                            try {
                                eventDispatcher.dispatchGlobalEventLocally(event);
                                backoff[0] = initialBackoff; // Reset backoff on success
                            } catch (Exception dispatchError) {
                                log.error("Error dispatching event {}", event, dispatchError);
                            }
                        });
            } catch (Exception e) {
                log.error("Error consuming event, backing off for {}ms", backoff[0], e);
                try {
                    Thread.sleep(backoff[0]);
                    backoff[0] = Math.min(maxBackoff, backoff[0] * 2); // Double the backoff time
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    log.info("Consumer interrupted during backoff");
                }
            }
        }

        log.info("runConsumer finishing");
    }
}
