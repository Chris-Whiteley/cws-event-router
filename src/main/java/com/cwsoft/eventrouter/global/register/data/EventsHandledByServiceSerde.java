package com.cwsoft.eventrouter.global.register.data;

import com.cwsoft.eventrouter.global.SerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("unused") // library class for use by messaging implementation
public class EventsHandledByServiceSerde {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules(); // Configure as needed

    /**
     * Serializes an EventsHandledByService object to a JSON string.
     *
     * @param eventsHandledByService the object to serialize
     * @return the JSON string representation of the object
     * @throws SerializationException if serialization fails
     */
    public static String serialize(EventsHandledByService eventsHandledByService) {
        if (eventsHandledByService == null) {
            log.error("Cannot serialize a null EventsHandledByService object");
            throw new IllegalArgumentException("EventsHandledByService cannot be null");
        }
        try {
            return objectMapper.writeValueAsString(eventsHandledByService);
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("Serialization error for EventsHandledByService: {}", eventsHandledByService, e);
            throw new SerializationException("Failed to serialize EventsHandledByService", e);
        }
    }

    /**
     * Deserializes a JSON string to an EventsHandledByService object.
     *
     * @param json the JSON string
     * @return the deserialized EventsHandledByService object
     * @throws SerializationException if deserialization fails
     */
    public static EventsHandledByService deserialize(String json) {
        if (json == null || json.isEmpty()) {
            log.error("Cannot deserialize a null or empty JSON string");
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        try {
            return objectMapper.readValue(json, EventsHandledByService.class);
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("Deserialization error for JSON: {}", json, e);
            throw new SerializationException("Failed to deserialize to EventsHandledByService object", e);
        }
    }
}
