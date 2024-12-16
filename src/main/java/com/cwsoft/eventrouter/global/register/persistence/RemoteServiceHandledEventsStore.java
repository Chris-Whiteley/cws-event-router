package com.cwsoft.eventrouter.global.register.persistence;

import com.cwsoft.eventrouter.global.register.data.EventsHandledByRemoteServices;

public interface RemoteServiceHandledEventsStore {
    EventsHandledByRemoteServices fetch();
    void save (EventsHandledByRemoteServices eventsHandledByRemoteServices);
}
