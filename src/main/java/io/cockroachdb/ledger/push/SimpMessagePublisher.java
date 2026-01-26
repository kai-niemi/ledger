package io.cockroachdb.ledger.push;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import io.cockroachdb.ledger.util.RateLimiter;

@Component
public class SimpMessagePublisher {
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    private final Map<Object, RateLimiter> rateLimiterMap = Collections.synchronizedMap(new HashMap<>());

    public <T> void convertAndSend(TopicName topic, T payload) {
        if (payload != null) {
            simpMessagingTemplate.convertAndSend(topic.value, payload);
        } else {
            simpMessagingTemplate.convertAndSend(topic.value, "");
        }
    }

    public <T> void convertAndSendThrottled(TopicName topic,
                                            T payload,
                                            Object throttleKey,
                                            int permitsPerSecond) {
        RateLimiter rateLimiter = rateLimiterMap.computeIfAbsent(throttleKey,
                o -> new RateLimiter(permitsPerSecond));
        if (rateLimiter.tryAcquire()) {
            convertAndSend(topic, payload);
        }
    }
}
