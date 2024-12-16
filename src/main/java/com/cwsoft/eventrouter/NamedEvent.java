package com.cwsoft.eventrouter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Set;

@Slf4j
@ToString (of = {"name"}, doNotUseGetters = true)
public class NamedEvent {

    @Getter
    private final String name;

    @Getter
    private boolean retryOnFailure;

    @Getter
    @Singular
    protected Set<String> destinationServices; // used to specify specific services as the destination of this event

    protected String siteInContext;

    public String getSiteInContext() {
        return (siteInContext == null)?"":siteInContext;
    }


    private String encoded;
    private byte[] encodedBytes;

    public NamedEvent(String name) {
        this.name = name;
        this.destinationServices = Collections.EMPTY_SET;
        this.siteInContext = "";
    }

    public NamedEvent(String name, Set<String> destinationServices) {
        this.name = name;
        this.destinationServices = destinationServices;
        this.siteInContext = "";
    }

    @Builder (builderMethodName = "namedBuilder")
    public NamedEvent(String name, boolean retryOnFailure, @Singular Set<String> destinationServices, String siteInContext) {
        this.name = name;
        this.retryOnFailure = retryOnFailure;
        this.destinationServices = destinationServices;
        this.siteInContext = (siteInContext==null)?"":siteInContext;
    }

    private static ObjectMapper objectMapper;

    /**
     * Note. We create our own ObjectMapper instance as can't use RebasoftApplicationContext
     * since NamedEvent is also used in RMC which does not have RebasoftApplicationContext as it uses Spring.
     * Also can't inject as this is value object rather than a Managed bean.
     *
     * @return ObjectMapper instance
     */
    static ObjectMapper getObjectMapper() {
        if (objectMapper == null) objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModule(new Jdk8Module())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        return objectMapper;
    }

    public static NamedEvent decode(String jsonEvent) {
        try {
            NamedEvent event;
            ObjectNode objectNode = (ObjectNode) getObjectMapper().readTree(jsonEvent);
            String name = objectNode.get("nm").asText();
            Class clazz = null;
            String sourceJson = null;
            Object source = null;

            if (objectNode.has("cls")) {
                clazz = getObjectMapper().readValue(objectNode.get("cls").asText(), Class.class);
                sourceJson = objectNode.get("src").asText();
            }

            if (sourceJson == null && !objectNode.has("frm")) {
                event = new NamedEvent(name);
            } else if (objectNode.has("frm")) {
                event = GlobalEvent.globalBuilder().name(name).source(null).sourceJson(sourceJson).sourceClass(clazz).fromServiceId(objectNode.get("frm").asText()).build();
            } else {
                source = getObjectMapper().readValue(sourceJson, clazz);
                event = Event.builder().name(name).source(source).build();
            }

            return event;
        } catch (IOException e) {
            log.error("Error decoding Event from JSON string {}", jsonEvent, e);
            return null;
        }
    }

    public String encode() throws JsonProcessingException {
        if (encoded != null) return encoded;

        ObjectNode objectNode = getObjectMapper().createObjectNode();
        objectNode.put("nm", name);

        if (this instanceof Event) {
            Event thisEvent = (Event) this;
            if (thisEvent.getSource() != null) {
                String cls = getObjectMapper().writeValueAsString(thisEvent.getSource().getClass());
                objectNode.put("cls", cls);
                String src = getObjectMapper().writeValueAsString(thisEvent.getSource());
                objectNode.put("src", src);
            }
        }

        if (this instanceof GlobalEvent) {
            GlobalEvent thisGlobalEvent = (GlobalEvent) this;
            objectNode.put("frm", thisGlobalEvent.getFromServiceId());
        }

        encoded = objectNode.toString();
        return encoded;
    }

    int getEncodedSize() throws JsonProcessingException, UnsupportedEncodingException {
        return getEncodedBytes().length;
    }

    private byte[] getEncodedBytes() throws JsonProcessingException, UnsupportedEncodingException {
        if (encodedBytes != null) return encodedBytes;
        encodedBytes = encode().getBytes("UTF8");
        return encodedBytes;
    }
}
