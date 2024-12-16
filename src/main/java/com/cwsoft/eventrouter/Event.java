package com.cwsoft.eventrouter;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;


/**
 * Value class used to pass events round
 *
 * @param <E> the event payload type
 */


@Getter
@ToString (callSuper = true, doNotUseGetters = true)
@EqualsAndHashCode

@Slf4j
public class Event<E> extends NamedEvent {
    protected E source;

    public Event(String name, E source) {
        super(name);
        this.source = source;
    }

    @Builder
    public Event(String name, E source, @Singular Set <String> destinationServices, String siteInContext) {
        super(name);
        this.source = source;
        this.destinationServices = destinationServices;
        this.siteInContext = (siteInContext == null)?"":siteInContext;
    }

    /**
     * This method of obtaining the source value is to overcome the problem when sending
     * a collection of objects (pojos) as an event.  Then we get the issue in use of generics and
     * jackson JSON has trouble de-serializing as it doesn't have access to the generic type
     * and the error java.util.LinkedHashMap cannot be cast to X is thrown.
     *
     *   see also .... https://www.baeldung.com/jackson-linkedhashmap-cannot-be-cast
     *
     * @param sourceTypeRef
     * @return the source object deserialized to the specified sourceTypeRef
     */
    public E getSource(TypeReference<E> sourceTypeRef) {
        return null;  // meant to be overridden
    }

    public Event(String name, E source, String destinationService) {
        this(name, source, Set.of(destinationService), null);
    }

}
