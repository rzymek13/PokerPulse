package prtech.com.pokerpulse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final String STATIC_WEB_APP_ORIGIN = "https://red-moss-00947f703.2.azurestaticapps.net";
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // natywny WebSocket (bez SockJS) - przydatne do testów z Postmanem / czystymi klientami WebSocket
        registry.addEndpoint("/test-ws")
                .setAllowedOriginPatterns("http://localhost:*", STATIC_WEB_APP_ORIGIN);

        // endpoint z SockJS (pozostawiamy dla frontendu który może z niego korzystać)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:*", STATIC_WEB_APP_ORIGIN)
                .withSockJS();
    }
}
