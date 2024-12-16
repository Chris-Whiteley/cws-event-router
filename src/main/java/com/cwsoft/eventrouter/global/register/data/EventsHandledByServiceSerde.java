package com.cwsoft.eventrouter.global.register.data;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("unused") // library class for use by messaging implementation
public class EventsHandledByServiceSerde {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Serializes an EventsHandledByService object to a JSON string.
     *
     * @param eventsHandledByService the object to serialize
     * @return the JSON string representation of the object
     * @throws SerializationException if serialization fails
     */
    public static String serialize(EventsHandledByService eventsHandledByService) {
        try {
            return objectMapper.writeValueAsString(eventsHandledByService);
        } catch (JsonProcessingException e) {
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
        try {
            return objectMapper.readValue(json, EventsHandledByService.class);
        } catch (JsonProcessingException e) {
            log.error("Deserialization to EventsHandledByService for json: {}", json, e);
            throw new SerializationException("Failed to deserialize to EventsHandledByService object", e);
        }
    }
}
