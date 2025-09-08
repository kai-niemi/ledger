package se.cockroachdb.ledger.push;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.RateLimiter;

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
                                            double permitsPerSecond) {
        RateLimiter rateLimiter = rateLimiterMap.computeIfAbsent(throttleKey,
                o -> RateLimiter.create(permitsPerSecond));
        if (rateLimiter.tryAcquire()) {
            convertAndSend(topic, payload);
        }
    }
}
