package com.auction.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Raw STOMP endpoint /ws (Nhi_2 client uses this)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // Raw STOMP endpoint /ws-auction (Nhu_2 client uses this)
        registry.addEndpoint("/ws-auction")
                .setAllowedOriginPatterns("*");
    }
}
