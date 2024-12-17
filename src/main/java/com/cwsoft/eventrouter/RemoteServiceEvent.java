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
    private String remoteServiceId;
    private Event event;

    public boolean isRetryable() {
        return event.isRetryOnFailure();
    }

}
