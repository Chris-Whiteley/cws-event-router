package com.cwsoft.eventrouter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;


@Setter
@Singleton
@Slf4j
public class EventDispatcher {

    private EventHandlers eventHandlers;

    @Inject
    public void setEventHandlers(EventHandlers eventHandlers) {
        this.eventHandlers = eventHandlers;
    }

    @Getter
    static EventDispatcher instanceOf;

    public EventDispatcher() {
        instanceOf = this;
    }

    // used for testing only
    public EventDispatcher(EventDispatcher eventDispatcher) {
        instanceOf = eventDispatcher;
    }

    public void registerLocalHandler(Object handlerBean, String methodName, String eventName) throws NoSuchMethodException {
        registerHandler(handlerBean, methodName, eventName, false);
    }

    public void registerGlobalHandler(Object handlerBean, String methodName, String eventName) throws NoSuchMethodException {
        registerHandler(handlerBean, methodName, eventName, true);
    }

    private void registerHandler(Object handlerBean, String methodName, String eventName, boolean global) throws NoSuchMethodException {
        if (handlerBean == null) throw new NullPointerException("handlerObject is null");
        if (methodName == null || methodName.isEmpty())
            throw new IllegalArgumentException("method name is null or empty");
        if (eventName == null || eventName.isEmpty()) throw new IllegalArgumentException("eventName is null or empty");

        Method handlerMethod;
        try {
            handlerMethod = handlerBean.getClass().getMethod(methodName, Event.class);
        } catch (NoSuchMethodException ex) {
            // is there a handler for this event which does not have an argument
            handlerMethod = handlerBean.getClass().getMethod(methodName);
        }

        SubscriberEndPoint.SubscriberEndPointBuilder subscriberEndPointBuilder =
                SubscriberEndPoint
                        .builder()
                        .forEvent(eventName)
                        .bean(handlerBean)
                        .method(handlerMethod)
                        .access(Access.LOCAL);

        if (global) {
            subscriberEndPointBuilder.access(Access.GLOBAL);
        }

        eventHandlers.registerSubscriber(subscriberEndPointBuilder.build());

        if (global) {
            dispatchEvent(new Event("GlobalEventsHandledUpdated", eventName));
        }
    }

    public DispatchNote generateDispatchNote(NamedEvent event) {
        var dispatchNoteBuilder = DispatchNote.builder();

        if (eventHandlers != null) {
            eventHandlers.get(event.getName())
                    .stream()
                    .filter(handler -> handlerInContext(handler, event))
                    .forEach(handler -> {
                                fillInDispatchNote(handler, dispatchNoteBuilder);
                            }
                    );
            dispatchNoteBuilder.event(event);
        }

        return dispatchNoteBuilder.build();
    }

    public DispatchNote dispatchEvent(NamedEvent event) {
        var dispatchNoteBuilder = DispatchNote.builder();

        if (eventHandlers != null) {
            eventHandlers.get(event.getName())
                    .stream()
                    .filter(handler -> handlerInContext(handler, event))
                    .forEach(handler -> {
                                log.trace("dispatching event {} to handler {}", event, handler);
                                handler.handle(event);
                                fillInDispatchNote(handler, dispatchNoteBuilder);
                            }
                    );
            dispatchNoteBuilder.event(event);
        }

        return dispatchNoteBuilder.build();
    }

    private boolean handlerInContext(Handler handler, NamedEvent namedEvent) {
       if (handler instanceof LocalHandler) return true;
       if (handler instanceof RemoteHandler) {
           var remoteHandler = (RemoteHandler) handler;
           if (namedEvent.getDestinationServices() != null && !namedEvent.getDestinationServices().isEmpty()) {
               return namedEvent.getDestinationServices().contains(remoteHandler.getRemoteService());
           } else {
               if (namedEvent.getSiteInContext().isBlank()) return true;
               return RemoteHandler.sitesInSameBranch(remoteHandler.getRemoteServicesSite(), namedEvent.getSiteInContext());
           }
       }
       return true;
    }

    private void fillInDispatchNote (Handler handler, DispatchNote.DispatchNoteBuilder dispatchNoteBuilder) {
        if (handler instanceof  LocalHandler) {
            dispatchNoteBuilder.localEndPoint(((LocalHandler) handler).getLocalEndPoint());
        } else if (handler instanceof  RemoteHandler) {
            dispatchNoteBuilder.remoteService(((RemoteHandler) handler).getRemoteService());
        }
    }

    public void dispatchGlobalEventLocally(NamedEvent event) {
        if (eventHandlers != null) {
            eventHandlers.getGlobalHandler(event.getName())
                    .forEach(handler -> {
                                log.trace("dispatching global event {} to local handler {}", event, handler);
                                handler.handle(event);
                            }
                    );
        }
    }
}
