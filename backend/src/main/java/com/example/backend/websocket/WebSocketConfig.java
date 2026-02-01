package com.example.backend.websocket;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.messaging.simp.config.MessageBrokerRegistry;
//import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
//import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
//import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
//
//@Configuration
//@EnableWebSocketMessageBroker
//public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
//
//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry registry) {
//        registry.enableSimpleBroker("/topic")
//        .setHeartbeatValue(new long[]{10000, 10000});
//        registry.setApplicationDestinationPrefixes("/app");
//    }
//
//    @Override
//    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        registry.addEndpoint("/ws")
//                .setAllowedOriginPatterns("*")
//                .withSockJS()
//                .setHeartbeatTime(25000)
//                .setDisconnectDelay(5000);
//    }
//}
//



import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler; // <--- Import this
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(25000)
                .setDisconnectDelay(5000);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 1. Create a Scheduler for the heartbeats
        ThreadPoolTaskScheduler te = new ThreadPoolTaskScheduler();
        te.setPoolSize(1);
        te.setThreadNamePrefix("wss-heartbeat-thread-");
        te.initialize(); // <--- Important: Initialize it!

        // 2. Pass the scheduler to the broker
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(te); // <--- This fixes the crash

        registry.setApplicationDestinationPrefixes("/app");
    }
}