package com.cwsoft.eventrouter;

public interface Handler {
    <E extends NamedEvent> void handle(E e);
}
