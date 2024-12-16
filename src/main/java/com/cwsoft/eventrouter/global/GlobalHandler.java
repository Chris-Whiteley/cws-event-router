package com.cwsoft.eventrouter.global;

import com.cwsoft.eventrouter.GlobalEvent;
import com.cwsoft.eventrouter.NamedEvent;
import com.cwsoft.eventrouter.RemoteHandler;
import com.cwsoft.eventrouter.RemoteServiceEvent;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EqualsAndHashCode (of = {"fromServiceId","toServiceId"})
@ToString
@Getter
@Builder
public class GlobalHandler implements RemoteHandler {
    private final String fromServiceId; //  ServerId (UUID as string) or Service Name
    private final String toServiceId;  // ServerId (UUID as string) or Service Name
    private final String remoteServicesSite;

    @Override
    public <E extends NamedEvent> void handle(E e) {
        try {

            GlobalEvent globalEvent = GlobalEvent.toGlobalEvent(e, fromServiceId);

            // note added this check for when we have a "targeted" service event i.e. an event is to be sent to specific service(s).
            if (globalEvent.getDestinationServices().isEmpty() || globalEvent.getDestinationServices().contains(toServiceId)) {
                RemoteServiceEvent remoteServiceEvent = RemoteServiceEvent.builder().event(globalEvent).remoteServiceId(toServiceId).build();
                log.trace("Global handler {} dispatching event {} using GlobalEventsProducer", this, e);
                GlobalEventsProducer.getInstanceOf().publish(remoteServiceEvent);
            }
        } catch (Exception ex) {
            log.error("Error handling Global event {}", e, ex);
        }
    }

    @Override
    public String getRemoteService() {
        return toServiceId;
    }
}
