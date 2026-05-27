package com.auction.server.service;

import com.auction.common.payload.NotificationMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyUser(Long userId, String type, String title, String message, Long itemId, Long auctionId) {
        if (userId == null) {
            return;
        }
        NotificationMessage notification = new NotificationMessage(type, title, message, itemId, auctionId);
        messagingTemplate.convertAndSend("/topic/user-" + userId, notification);
    }
}
