package com.cwsoft.eventrouter.global.register.data;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unused") // library class for use by messaging implementation
public class EventsHandledByServiceSerde {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Serializes an EventsHandledByService object to a JSON string.
     *
     * @param eventsHandledByService the object to serialize
     * @return the JSON string representation of the object
     * @throws JsonProcessingException if serialization fails
     */
    public static String serialize(EventsHandledByService eventsHandledByService) throws JsonProcessingException {
        return objectMapper.writeValueAsString(eventsHandledByService);
    }

    /**
     * Deserializes a JSON string to an EventsHandledByService object.
     *
     * @param json the JSON string
     * @return the deserialized EventsHandledByService object
     * @throws JsonProcessingException if deserialization fails
     */
    public static EventsHandledByService deserialize(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, EventsHandledByService.class);
    }
}
