package com.cwsoft.eventrouter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

@EqualsAndHashCode
@ToString(of = {"handlerObject", "handlerMethod"})
@Getter
@Slf4j
public abstract class LocalHandler implements Handler {
    final Object handlerObject;
    final Method handlerMethod;
    int noOfParameters;

    public LocalHandler(Object handlerObject, Method handlerMethod, int noOfParameters) {
        this.handlerObject = handlerObject;
        this.handlerMethod = handlerMethod;
        this.noOfParameters = noOfParameters;

        if (noOfParameters > 1) {
            throw new IllegalArgumentException(String.format("The handle event method %s of class %s has too many parameters", handlerMethod, handlerObject.getClass()));
        }
    }

    public String getLocalEndPoint() {
        return this.handlerObject.getClass().getSimpleName() + '.' + this.handlerMethod;
    }

    protected void invoke(NamedEvent event) {
        try {
            handlerMethod.setAccessible(true);

            if (noOfParameters == 0) {
                handlerMethod.invoke(handlerObject);
            } else {
                handlerMethod.invoke(handlerObject, event);
            }
        } catch (Exception ex) {
            log.error("Error dispatching event {} to handler {}", event, this, ex);
        }
    }
}