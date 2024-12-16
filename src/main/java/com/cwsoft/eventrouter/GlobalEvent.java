package com.cwsoft.eventrouter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Set;

@Slf4j
@ToString(callSuper = true, doNotUseGetters = true)
public class GlobalEvent<E> extends Event<E> {

    @Getter
    private final String fromServiceId;

    @Setter
    private String sourceJson;
    @Setter
    private Class sourceClass;

    @Override
    public E getSource(TypeReference<E> sourceTypeRef) {
        if (this.source == null) {
            try {
                this.source = (E) getObjectMapper().readValue(sourceJson, sourceTypeRef);
            } catch (JsonProcessingException e) {
                log.error("Error decoding Event from JSON string {}", sourceJson, e);
                return null;
            }
        }

        return this.source;
    }

    public E getSource() {
        if (this.source == null && sourceJson != null) {
            try {
                this.source = (E) getObjectMapper().readValue(sourceJson, sourceClass);
            } catch (JsonProcessingException e) {
                log.error("Error decoding Event from JSON string {}", sourceJson, e);
                return null;
            }
        }

        return this.source;
    }

    public static GlobalEvent toGlobalEvent(NamedEvent e, String fromServiceId) {
        GlobalEventBuilder builder = GlobalEvent.globalBuilder();
        builder.name(e.getName());

        if (e instanceof Event) {
            Event event = (Event) e;
            builder.destinationServices(event.destinationServices);
            builder.source(event.getSource());
        } else {
            builder.destinationServices(Collections.EMPTY_SET);
        }

        builder.fromServiceId(fromServiceId);
        return builder.build();
    }

    @Builder(builderMethodName = "globalBuilder")
    GlobalEvent(String name, E source, Set<String> destinationServices, String fromServiceId, String sourceJson, Class sourceClass) {
        super(name, source, destinationServices, null);
        this.sourceJson = sourceJson;
        this.sourceClass = sourceClass;
        this.fromServiceId = fromServiceId;
    }
}
