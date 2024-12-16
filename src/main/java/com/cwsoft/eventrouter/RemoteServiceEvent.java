package com.cwsoft.eventrouter;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * An event that needs to be sent to a remote service
 */
@EqualsAndHashCode
@ToString
@Getter
@Builder
public class RemoteServiceEvent {
    private static final String EVENTS_FOR_SERVICE_TOPIC_NAME = "events.dispatch.events_for_service";
    private String remoteServiceId;
    private Event event;

    public String getDestination() {
        return getDestination(remoteServiceId);
    }

    public boolean isRetryable() {
        return event.isRetryOnFailure();
    }

    public static String getDestination(String forServiceId) {
        return EVENTS_FOR_SERVICE_TOPIC_NAME + "_" + forServiceId.replace(" ", "_");
    }

}
