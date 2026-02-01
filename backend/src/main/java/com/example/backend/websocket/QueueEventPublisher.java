package com.example.backend.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishQueueUpdate(QueueEvent event) {
        messagingTemplate.convertAndSend("/topic/queue-updates", event);
        messagingTemplate.convertAndSend("/topic/display-board", event);
    }
    public void publishToPatient(String tokenNumber, QueueEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/patient/" + tokenNumber,
                event
        );
    }
    public void publishCounterUpdate(QueueEvent event) {
        messagingTemplate.convertAndSend("/topic/counter-updates", event);
    }
}

