package com.cwsoft.eventrouter;

import javax.inject.Singleton;

@Singleton
public class ThreadLocalHandlerFactory implements LocalHandlerFactory {

    @Override
    public LocalHandler newHandler(SubscriberEndPoint subscriberEndPoint) {
        return ThreadLocalHandler.builder()
                .handledEventName(subscriberEndPoint.getForEvent())
                .handlerObject(subscriberEndPoint.getBean())
                .handlerMethod(subscriberEndPoint.getMethod())
                .noOfParameters(subscriberEndPoint.getParameterCount())
                .build();
    }
}
