package com.cwsoft.eventrouter.global;

import com.cwsoft.eventrouter.EventHandler;
import com.cwsoft.eventrouter.RemoteServiceEvent;
import com.cwsoft.messaging.Producer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides ability to publish global events for remote services. Handles retries for events marked as retryable
 * when the messaging system is temporarily unavailable.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class GlobalEventsProducer {

    private final Producer<RemoteServiceEvent> producer;
    private final BlockingQueue<RemoteServiceEvent> retryableQueue = new ArrayBlockingQueue<>(500_000);

    private volatile boolean messagingAvailable = true;
    private static final int RETRY_DELAY_MS = 5000;

    @Getter
    private static GlobalEventsProducer instanceOf;

    @EventHandler(name = "onStartup")
    public void init() {
        instanceOf = this;
        startProducer();
    }

    public void publish(RemoteServiceEvent event) {
        try {
            if (!event.isRetryable() && !messagingAvailable) {
                log.warn("Dropping non-retryable event as messaging is unavailable: {}", event);
                return;
            }

            if (!messagingAvailable) {
                retryableQueue.put(event);
            } else {
                sendEvent(event);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while adding event to the retryable queue: {}", event, e);
        }
    }

    private void startProducer() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdownNow));
        executorService.submit(this::processEvents);
    }

    private void processEvents() {
        Thread.currentThread().setName("G-EVT-PRODUCER");
        log.info("GlobalEventsProducer started");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                RemoteServiceEvent event = retryableQueue.take();

                if (!isMessagingAvailable()) {
                    log.warn("Messaging system unavailable, re-queueing event: {}", event);
                    Thread.sleep(RETRY_DELAY_MS);
                    retryableQueue.put(event); // Re-queue the event for later retry.
                } else {
                    sendEvent(event);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("GlobalEventsProducer shutting down");
            } catch (Exception e) {
                log.error("Unexpected error while processing events", e);
            }
        }
    }

    private void sendEvent(RemoteServiceEvent event) {
        try {
            producer.produce(event);
            messagingAvailable = true; // Mark as available on success.
        } catch (Exception e) {
            log.error("Failed to send event, marking messaging as unavailable: {}", event, e);
            messagingAvailable = false;
            if (event.isRetryable()) {
                retryableQueue.offer(event);
            }
        }
    }

    private boolean isMessagingAvailable() {
        // Implement a meaningful check here, such as a ping to the messaging system.
        return messagingAvailable;
    }
}
