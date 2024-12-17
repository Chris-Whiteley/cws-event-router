package com.cwsoft.eventrouter.global.register.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EventsHandledByRemoteServices implements Iterable<EventsHandledByService> {
    private final Map<String, EventsHandledByService> handledEventsMap;

    public EventsHandledByRemoteServices() {
        this.handledEventsMap = new HashMap<>();
    }

    public EventsHandledByService get(String serviceId, String serviceSite) {
        if (handledEventsMap.containsKey(serviceId)) return handledEventsMap.get(serviceId);
        return EventsHandledByService.builder()
                .serviceId(serviceId)
                .serviceSite(serviceSite).build();
    }

    public void put(EventsHandledByService remoteServiceHandledEvents) {
        handledEventsMap.put(remoteServiceHandledEvents.getServiceId(), remoteServiceHandledEvents);
    }

    @Override
    public Iterator<EventsHandledByService> iterator() {
        return handledEventsMap.values().iterator();
    }

    @Override
    public String toString() {
        return "EventsHandledByRemoteServices{" +
                "handledEventsMap=" + handledEventsMap +
                '}';
    }
}
