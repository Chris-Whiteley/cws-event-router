package com.cwsoft.eventrouter.global.register;

import com.cwsoft.eventrouter.EventHandler;
import com.cwsoft.eventrouter.EventHandlers;
import com.cwsoft.eventrouter.RemoteHandler;
import com.cwsoft.eventrouter.global.GlobalHandler;
import com.cwsoft.eventrouter.global.register.data.EventsHandledByRemoteServices;
import com.cwsoft.eventrouter.global.register.data.EventsHandledByService;
import com.cwsoft.eventrouter.global.register.persistence.RemoteServiceHandledEventsStore;
import com.cwsoft.messaging.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

/**
 * The HandlersRegistrar class is responsible for managing global event handlers for remote services.
 * It registers handlers based on events handled by remote services and dynamically updates them
 * when there are changes to the events handled by those services.
 * <p>
 * Responsibilities:
 * - Listen for a service registration event to initialize the service's site name.
 * - Register initial event handlers for remote services based on saved state.
 * - Dynamically update handlers when changes are detected in the events handled by remote services.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class HandlersRegistrar {

    /**
     * Event handlers registry for managing event-handler mappings
     */
    @NonNull
    private final EventHandlers eventHandlers;

    /**
     * Stream for processing updates about events handled by remote services
     */
    @NonNull
    private final Stream<String, EventsHandledByService> stream;

    /**
     * Persistent store for saving and fetching the current state of handled events
     */
    @NonNull
    private final RemoteServiceHandledEventsStore handledEventsStore;

    /**
     * Unique identifier for the current service
     */
    @NonNull
    private final String serviceId;

    /**
     * The site name of the current service
     */
    @NonNull
    private final String serviceSiteName;

    /**
     * Flag to indicate if the stream is active
     */
    private volatile boolean streamActive = false;

    /**
     * Handles the "onStartup" event.
     * Initializes the process of managing global event handlers for remote services.
     */
    @EventHandler(name = "onStartup")
    public void init() {
        if (serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }

        if (serviceSiteName.isBlank()) {
            throw new IllegalArgumentException("serviceSiteName must not be null or blank");
        }

        log.info("Initializing HandlersRegistrar...");
        startHandledByRemoteServicesConsumer();
    }

    /**
     * Starts processing updates about events handled by remote services.
     * Registers initial handlers and dynamically updates them based on the stream.
     */
    private synchronized void startHandledByRemoteServicesConsumer() {
        if (streamActive) {
            log.warn("Stream is already running. Ignoring start request.");
            return;
        }

        log.info("Starting stream to consume updates for handled events by remote services.");
        streamActive = true;

        // Fetch the current state of event handlers from the store
        EventsHandledByRemoteServices currentEventsHandledByRemoteServices = handledEventsStore.fetch();

        // Register handlers based on the saved state
        registerHandlers(currentEventsHandledByRemoteServices);

        // Process updates from the stream
        stream.filter((serviceId, serviceHandledEvents) -> !serviceId.equals(this.serviceId))
                .forEach((remoteServiceId, remoteServiceHandledEvents) -> {
                    try {
                        var currentRemoteServiceHandledEvents = currentEventsHandledByRemoteServices
                                .get(remoteServiceId, remoteServiceHandledEvents.getServiceSite());
                        updateHandlersForService(currentRemoteServiceHandledEvents, remoteServiceHandledEvents);
                        currentEventsHandledByRemoteServices.put(remoteServiceHandledEvents);
                        handledEventsStore.save(currentEventsHandledByRemoteServices);
                    } catch (Exception e) {
                        log.error("Error processing events for remoteServiceId {}", remoteServiceId, e);
                        stopStreamWithError(e);
                    }
                });

        try {
            stream.start();
            log.info("Stream started successfully.");
        } catch (Exception e) {
            log.error("Failed to start the stream.", e);
            streamActive = false;
        }
    }

    /**
     * Stops the stream gracefully, ensuring all resources are released.
     */
    public synchronized void stopStream() {
        if (!streamActive) {
            log.warn("Stream is not running. Ignoring stop request.");
            return;
        }

        try {
            log.info("Stopping the stream.");
            stream.stop();
        } catch (Exception e) {
            log.error("Error while stopping the stream.", e);
        } finally {
            streamActive = false;
            log.info("Stream stopped successfully.");
        }
    }

    private static final int MAX_RESTART_ATTEMPTS = 5;
    private int restartAttempts = 0;

    /**
     * Stops the stream in case of an error and attempts to restart it.
     *
     * @param error the error that caused the stream to stop
     */
    private void stopStreamWithError(Exception error) {
        log.error("Stopping stream due to an error: {}", error.getMessage(), error);
        stopStream();
        if (restartAttempts < MAX_RESTART_ATTEMPTS) {
            restartStream();
        } else {
            log.error("Max restart attempts reached. Stream will not be restarted.");
        }
    }

    /**
     * Attempts to restart the stream with a delay.
     */
    private void restartStream() {
        restartAttempts++;
        long delay = (long) Math.pow(2, restartAttempts) * 1000; // Exponential backoff
        log.info("Attempting to restart the stream. Attempt: {}. Delaying for {} ms", restartAttempts, delay);

        try {
            Thread.sleep(delay);
            startHandledByRemoteServicesConsumer();
            log.info("Stream restarted successfully.");
            restartAttempts = 0; // Reset on success
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting to restart the stream.", e);
        } catch (Exception e) {
            log.error("Failed to restart the stream.", e);
            stopStreamWithError(e);
        }
    }

    /**
     * Registers initial event handlers for remote services based on the saved state.
     *
     * @param currentEventsHandledByRemoteServices the current state of events handled by remote services
     */
    private void registerHandlers(EventsHandledByRemoteServices currentEventsHandledByRemoteServices) {
        currentEventsHandledByRemoteServices.forEach(remoteServiceHandledEvents -> {
            remoteServiceHandledEvents.getHandledEvents()
                    .forEach(event -> eventHandlers.add(
                            event,
                            GlobalHandler.builder()
                                    .fromServiceId(serviceId)
                                    .toServiceId(remoteServiceHandledEvents.getServiceId())
                                    .remoteServicesSite(remoteServiceHandledEvents.getServiceSite())
                                    .build()
                    ));
        });
    }

    /**
     * Updates the event handlers for a specific remote service by comparing new and existing events.
     *
     * @param currentEventsHandled the current set of events handled by the remote service
     * @param newEventsHandled     the updated set of events handled by the remote service
     */
    private void updateHandlersForService(
            EventsHandledByService currentEventsHandled,
            EventsHandledByService newEventsHandled) {

        var remoteServiceId = newEventsHandled.getServiceId();
        var remoteSiteName = newEventsHandled.getServiceSite();

        log.trace("Got global events for remoteServiceId {} at site {} global events = {}",
                remoteServiceId, remoteSiteName, newEventsHandled.getHandledEvents());

        // Check if the remote service site is in the same branch as the current service site
        if (RemoteHandler.sitesInSameBranch(this.serviceSiteName, newEventsHandled.getServiceSite())) {
            log.debug("This service's site {} is in the same branch as the remote service {}. Remote service's site is {}",
                    this.serviceSiteName, remoteServiceId, remoteSiteName);

            // Calculate added and removed events
            Set<String> addedEvents = new HashSet<>(newEventsHandled.getHandledEvents());
            addedEvents.removeAll(currentEventsHandled.getHandledEvents());

            Set<String> deletedEvents = new HashSet<>(currentEventsHandled.getHandledEvents());
            deletedEvents.removeAll(newEventsHandled.getHandledEvents());

            // Add new events to the handlers
            if (!addedEvents.isEmpty()) {
                log.trace("Adding the following events: {}", addedEvents);
                addedEvents.forEach(event -> eventHandlers.add(
                        event,
                        GlobalHandler.builder()
                                .fromServiceId(serviceId)
                                .toServiceId(remoteServiceId)
                                .remoteServicesSite(remoteSiteName)
                                .build()
                ));
            }

            // Remove deleted events from the handlers
            if (!deletedEvents.isEmpty()) {
                log.trace("Removing the following events: {}", deletedEvents);
                deletedEvents.forEach(event -> eventHandlers.remove(
                        event,
                        GlobalHandler.builder()
                                .fromServiceId(serviceId)
                                .toServiceId(remoteServiceId)
                                .remoteServicesSite(remoteSiteName)
                                .build()
                ));
            }
        } else {
            // Remote service site is not in the same branch
            log.debug("This service's site {} is NOT in the same branch as the remote service {}. Remote service's site is {}",
                    this.serviceSiteName, remoteServiceId, remoteSiteName);
        }
    }
}
