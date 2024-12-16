package com.cwsoft.eventrouter;

public interface LocalHandlerFactory {
    LocalHandler newHandler (SubscriberEndPoint subscriberEndPoint);
}
