package com.cwsoft.eventrouter.global.register;

import com.cwsoft.eventrouter.EventHandler;
import com.cwsoft.eventrouter.EventHandlers;
import com.cwsoft.eventrouter.global.register.data.EventsHandledByService;
import com.cwsoft.messaging.Producer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Produces message about global events handled by this service,
 * intended to inform other services about this service's event-handling capabilities.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class HandledProducer {

    @NonNull
    private final Producer<EventsHandledByService> producer;
    @NonNull
    private final EventHandlers eventHandlers;
    @NonNull
    private final String serviceId;
    @NonNull
    private final String serviceSiteName;

    private volatile EventsHandledByService latestData;

    private final ScheduledExecutorService retryExecutor = Executors.newSingleThreadScheduledExecutor();
    private static final long INITIAL_RETRY_INTERVAL_MS = 5000;
    private static final long MAX_RETRY_INTERVAL_MS = 60000;

    private long currentRetryIntervalMs = INITIAL_RETRY_INTERVAL_MS;

    @EventHandler(name = "onStartup")
    public void init() {
        if (serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }

        if (serviceSiteName.isBlank()) {
            throw new IllegalArgumentException("serviceSiteName must not be null or blank");
        }

        log.info("Initializing HandledProducer...");
        produceGlobalEventsHandledByThisService();
        scheduleRetryTask();
    }

    @EventHandler(name = "onShutdown")
    public void onShutdown() {
        log.info("Shutting down HandledProducer...");
        retryExecutor.shutdown();
        try {
            if (!retryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                retryExecutor.shutdownNow();
                log.warn("Forced shutdown of retry executor");
            }
        } catch (InterruptedException e) {
            retryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @EventHandler(name = "GlobalEventsHandledUpdated")
    public void produceGlobalEventsHandledByThisService() {
        latestData = createGlobalEventsHandled();
        attemptProduce();
    }

    private EventsHandledByService createGlobalEventsHandled() {
        return EventsHandledByService.builder()
                .serviceId(serviceId)
                .serviceSite(serviceSiteName)
                .handledEvents(eventHandlers.getGlobalEventsHandled())
                .build();
    }

    private void attemptProduce() {
        if (latestData == null) {
            return; // Nothing to send
        }
        try {
            producer.produce(latestData);
            log.info("Successfully produced global events handled by this service.");
            latestData = null; // Clear the data after successful send
            currentRetryIntervalMs = INITIAL_RETRY_INTERVAL_MS; // Reset retry interval on success
        } catch (Exception e) {
            log.warn("Failed to produce message, will retry later. Error: {}", e.getMessage());
        }
    }

    private void scheduleRetryTask() {
        retryExecutor.scheduleWithFixedDelay(() -> {
            if (latestData != null) {
                log.info("Retrying to produce global events...");
                attemptProduceWithExponentialBackoff();
            }
        }, INITIAL_RETRY_INTERVAL_MS, INITIAL_RETRY_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void attemptProduceWithExponentialBackoff() {
        if (latestData == null) {
            return; // Nothing to retry
        }

        try {
            producer.produce(latestData);
            log.info("Successfully retried and produced global events.");
            latestData = null; // Clear the data after successful send
            currentRetryIntervalMs = INITIAL_RETRY_INTERVAL_MS; // Reset retry interval on success
        } catch (Exception e) {
            log.warn("Retry failed: {}", e.getMessage());
            increaseRetryInterval();
        }
    }

    private void increaseRetryInterval() {
        currentRetryIntervalMs = Math.min(currentRetryIntervalMs * 2, MAX_RETRY_INTERVAL_MS);
        log.info("Increasing retry interval to {} ms", currentRetryIntervalMs);

        // Reschedule the retry task with the updated interval
        retryExecutor.schedule(this::retryProduce, currentRetryIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void retryProduce() {
        if (latestData != null) {
            log.info("Retrying to produce global events...");
            attemptProduceWithExponentialBackoff();
        }
    }
}

