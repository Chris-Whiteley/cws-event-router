package com.cwsoft.eventrouter;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Getter
@Slf4j
public class ThreadLocalHandler extends LocalHandler {

    private final String handledEventName;
    private final BlockingQueue<NamedEvent> eventQueue = new ArrayBlockingQueue<>(10_0000);

    @Builder
    private ThreadLocalHandler(String handledEventName, Object handlerObject, Method handlerMethod, int noOfParameters) {
       super(handlerObject, handlerMethod, noOfParameters);
       this.handledEventName = handledEventName;

        // now set up queue monitor thread to monitor the queue for events
        Thread queueConsumerThread = new Thread(() -> runQueueConsumer());
        queueConsumerThread.start();
    }

    private void runQueueConsumer() {
        Thread.currentThread().setName("EVT-" + handledEventName + "-" + handlerMethod.getName());

        var timeLastQueueSizeReport = System.currentTimeMillis();

        while (!Thread.interrupted()) {
            NamedEvent event = null;
            try {
                event = eventQueue.take();
                invoke(event);

                if (eventQueue.size() > 10 && (System.currentTimeMillis() - timeLastQueueSizeReport) > 60_000) {
                    log.info("event queue size is > 10, size is {}", eventQueue.size());
                    timeLastQueueSizeReport = System.currentTimeMillis();
                }


            } catch (InterruptedException e) {
            } catch (Exception e) {
                log.error ("error processing event from queue, {}", event);
            }
        }
    }

    @Override
    public <E extends NamedEvent> void handle(E e){
        try {
             eventQueue.put(e);
        } catch (InterruptedException interruptedException) {
        }
    }
}
