package com.cwsoft.eventrouter;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;

import java.lang.reflect.Method;
import java.util.Set;

@Getter
@Builder
@EqualsAndHashCode
public class SubscriberEndPoint {
    private final String forEvent;
    private final Object bean;
    private final Method method;
    @Singular("access")
    private final Set<Access> accessSet;

    public int getParameterCount() {
        return method.getParameterCount();
    }

    public boolean hasGlobalAccess() {
        return accessSet.contains(Access.GLOBAL);
    }

    public boolean hasLocalAccess() {
        return accessSet.contains(Access.LOCAL);
    }
}
