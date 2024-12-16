package com.cwsoft.eventrouter.global.register.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EventsHandledByRemoteServices implements Iterable<EventsHandledByService> {
    private final Map<String, EventsHandledByService> handledEventsMap;

    public EventsHandledByRemoteServices() {
        this.handledEventsMap = new HashMap<>();
    }

    //    public EventsHandledByRemoteServices(Map<String, RemoteServiceHandledEvents> handledEventsMap) {
//        this.handledEventsMap = handledEventsMap != null ? new HashMap<>(handledEventsMap) : new HashMap<>();
//    }
//
//    public Map<String, RemoteServiceHandledEvents> getHandledEventsMap() {
//        return handledEventsMap;
//    }
//
//    public void setHandledEventsMap(Map<String, RemoteServiceHandledEvents> handledEventsMap) {
//        this.handledEventsMap.clear();
//        if (handledEventsMap != null) {
//            this.handledEventsMap.putAll(handledEventsMap);
//        }
//    }
//
//    public void addOrUpdateHandledEvent(String serviceId, RemoteServiceHandledEvents events) {
//        handledEventsMap.put(serviceId, events);
//    }
//
//    public void removeHandledEvent(String serviceId) {
//        handledEventsMap.remove(serviceId);
//    }
//
    public EventsHandledByService get(String serviceId, String serviceSite) {
        if (handledEventsMap.containsKey(serviceId)) return handledEventsMap.get(serviceId);
        return EventsHandledByService.builder()
                .serviceId(serviceId)
                .serviceSite(serviceSite).build();
    }

    public void put(EventsHandledByService remoteServiceHandledEvents) {
        handledEventsMap.put(remoteServiceHandledEvents.getServiceId(), remoteServiceHandledEvents);
    }

//    public Collection<RemoteServiceHandledEvents> getAllHandledEvents() {
//        return handledEventsMap.values();
//    }

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
