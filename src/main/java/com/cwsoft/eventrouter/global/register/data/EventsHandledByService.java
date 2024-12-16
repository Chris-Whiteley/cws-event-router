package com.cwsoft.eventrouter.global.register.data;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents the global events handled by a service.
 */
@Getter
public class EventsHandledByService {
    private final String serviceId;          // The ID of the service
    private final String serviceSite;        // The location/site of the service
    private final Collection<String> handledEvents; // The global events handled by the service

    @Builder
    public EventsHandledByService(String serviceId, String serviceSite, Collection<String> handledEvents) {
        if (serviceId == null || serviceId.isBlank())
            throw new IllegalArgumentException("Service ID cannot be null or blank");
        if (serviceSite == null || serviceSite.isBlank())
            throw new IllegalArgumentException("Service site cannot be null or blank");

        this.serviceId = serviceId;
        this.serviceSite = serviceSite;
        this.handledEvents = (handledEvents != null) ? handledEvents : new ArrayList<>();
    }

    /**
     * Returns the size of the handled events collection.
     *
     * @return the size of the handled events, or 0 if the collection is null
     */
    public int size() {
        return (handledEvents != null) ? handledEvents.size() : 0;
    }
}
