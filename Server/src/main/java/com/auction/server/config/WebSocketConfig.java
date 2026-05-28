package com.auction.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

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
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        // Hỗ trợ convert các tin nhắn dạng String thuần túy
        messageConverters.add(new StringMessageConverter());

        // Hỗ trợ convert các tin nhắn dạng JSON (Object) tự động bằng Jackson
        messageConverters.add(new ByteArrayMessageConverter());

        return false; // Trả về false để Spring không ghi đè các converter mặc định khác
    }
}