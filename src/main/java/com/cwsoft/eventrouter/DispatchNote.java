package com.cwsoft.eventrouter;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;

@Getter
@ToString
@Builder
public class DispatchNote {
    private final NamedEvent event;

    @Singular
    private final Collection<String> localEndPoints;
    @Singular
    private final Collection<String> remoteServices;

    public Collection<String> getLocalEndPoints() {
        return new ArrayList<>(this.localEndPoints);
    }

    public Collection<String> getRemoteServices() {
        return new ArrayList<>(this.remoteServices);
    }
}
