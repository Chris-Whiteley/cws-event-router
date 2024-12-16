package com.cwsoft.eventrouter;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe class to manage and maintain local and global event handlers for a service.
 */
@Singleton
@Slf4j
public class EventHandlers {
    public final Class<? extends Annotation> END_POINT_ANNOTATION = EventHandler.class;

    private LocalHandlerFactory handlerFactory;

    @Inject
    public void setHandlerFactory(LocalHandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }

    /**
     * Thread-safe map of event names to sets of handlers.
     */
    private final Map<String, Set<Handler>> handlersMap = new ConcurrentHashMap<>();

    /**
     * Thread-safe set of global events handled by the service.
     */
    @Getter
    private final Set<String> globalEventsHandled = ConcurrentHashMap.newKeySet();

    /**
     * Thread-safe map of global handlers.
     */
    private final Map<String, Set<LocalHandler>> globalHandlersMap = new ConcurrentHashMap<>();

    /**
     * Retrieves all handlers for a given event.
     *
     * @param forEvent the event name.
     * @return a collection of handlers.
     */
    public Collection<Handler> get(String forEvent) {
        return handlersMap.getOrDefault(forEvent, Collections.emptySet());
    }

    /**
     * Adds a handler for a given event.
     *
     * @param eventName the event name.
     * @param handler   the handler to add.
     */
    public void add(String eventName, Handler handler) {
        log.trace("Adding handler for eventName = {}, handler = {}", eventName, handler);
        handlersMap.computeIfAbsent(eventName, k -> ConcurrentHashMap.newKeySet()).add(handler);
    }

    /**
     * Adds a global handler for a given event.
     *
     * @param eventName    the event name.
     * @param localHandler the local handler to add.
     */
    private void addGlobal(String eventName, LocalHandler localHandler) {
        log.trace("Adding global handler for eventName = {}, localHandler = {}", eventName, localHandler);
        globalHandlersMap.computeIfAbsent(eventName, k -> ConcurrentHashMap.newKeySet()).add(localHandler);
        globalEventsHandled.add(eventName);
    }

    /**
     * Retrieves all global handlers for a given event.
     *
     * @param forEvent the event name.
     * @return a collection of global handlers.
     */
    public Collection<LocalHandler> getGlobalHandler(String forEvent) {
        return globalHandlersMap.getOrDefault(forEvent, Collections.emptySet());
    }

    /**
     * Removes a handler for a given event.
     *
     * @param eventName the event name.
     * @param handler   the handler to remove.
     */
    public synchronized void remove(String eventName, Handler handler) {
        log.trace("Removing handler for eventName = {}, handler = {}", eventName, handler);
        Set<Handler> handlers = handlersMap.get(eventName);
        if (handlers != null) {
            handlers.remove(handler);
            if (handlers.isEmpty()) {
                handlersMap.remove(eventName);
            }
        }
    }

    /**
     * Initializes the event handlers by scanning for annotated subscriber end points.
     *
     * @param beanSupplier the bean supplier.
     */
    public void init(BeanSupplier beanSupplier) {
        try {
            log.info("Initializing EventDispatcher - scanning for subscriber end point handlers.");

            getSubscribers(beanSupplier).forEach(this::registerSubscriber);
        } catch (Exception ex) {
            log.error("Error while scanning for subscriber end points", ex);
        }
    }

    private Collection<SubscriberEndPoint> getSubscribers(BeanSupplier beanSupplier) {
        Collection<SubscriberEndPoint> subscribers = new ArrayList<>();

        getAllEndPoints(beanSupplier).forEach(endPoint -> {
            EventHandler[] eventHandlerAnnotations = endPoint.method.getAnnotationsByType(EventHandler.class);

            for (EventHandler eventHandlerAnnotation : eventHandlerAnnotations) {
                for (String event : eventHandlerAnnotation.name()) {
                    subscribers.add(SubscriberEndPoint.builder()
                            .forEvent(event)
                            .bean(endPoint.bean)
                            .method(endPoint.method)
                            .accessSet(Arrays.asList(eventHandlerAnnotation.access()))
                            .build());
                }
            }
        });

        return subscribers;
    }

    private Collection<EndPoint> getAllEndPoints(BeanSupplier beanSupplier) {
        Collection<EndPoint> endPoints = new HashSet<>();

        beanSupplier.getAllBeans().forEach(bean ->
                getAllMethods(bean.getClass()).forEach(method -> {
                    if (method.isAnnotationPresent(END_POINT_ANNOTATION)) {
                        endPoints.add(
                                EndPoint.builder()
                                        .bean(bean)
                                        .method(method)
                                        .build()
                        );
                    }
                }));

        return endPoints;
    }

    void registerSubscriber(SubscriberEndPoint subscriberEndPoint) {
        LocalHandler localHandler = handlerFactory.newHandler(subscriberEndPoint);

        if (subscriberEndPoint.hasLocalAccess()) {
            this.add(subscriberEndPoint.getForEvent(), localHandler);
        }

        if (subscriberEndPoint.hasGlobalAccess()) {
            this.addGlobal(subscriberEndPoint.getForEvent(), localHandler);
            log.trace("Added global event {}, globalEventsHandled now contains {}", subscriberEndPoint.getForEvent(), globalEventsHandled);
        }
    }

    private List<Method> getAllMethods(Class<?> forClass) throws FindEndPointException {
        try {
            List<Method> allMethods = new ArrayList<>();
            Class<?> clazz = forClass;

            while (clazz != null) {
                allMethods.addAll(Arrays.asList(clazz.getDeclaredMethods()));
                clazz = clazz.getSuperclass();
            }

            return allMethods;
        } catch (Exception | NoClassDefFoundError ex) {
            String msg = String.format("Failed to obtain methods of class %s", forClass);
            throw new FindEndPointException(msg, ex);
        }
    }

    @Builder
    private static class EndPoint {
        private final Object bean;
        private final Method method;
    }
}
