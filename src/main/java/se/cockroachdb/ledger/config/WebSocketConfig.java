package se.cockroachdb.ledger.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ledger-service")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHttpMessageCacheSize(128)
                .setDisconnectDelay(15000)
                .setClientLibraryUrl("/webjars/sockjs-client/1.5.1/sockjs.min.js");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
//        registration.taskExecutor().maxPoolSize(256);
    }
}

