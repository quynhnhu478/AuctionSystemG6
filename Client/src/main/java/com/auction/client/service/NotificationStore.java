package com.auction.client.service;

import com.auction.common.payload.NotificationMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NotificationStore {
    private static final List<NotificationMessage> notifications = new ArrayList<>();
    private static boolean unread;

    private NotificationStore() {
    }

    public static synchronized void add(NotificationMessage notification) {
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(LocalDateTime.now());
        }
        notifications.add(0, notification);
        unread = true;
        AppEventBus.emit("NOTIFICATION_UNREAD_CHANGED", true);
    }

    public static synchronized List<NotificationMessage> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(notifications));
    }

    public static synchronized boolean hasUnread() {
        return unread;
    }

    public static synchronized void markAllRead() {
        unread = false;
        AppEventBus.emit("NOTIFICATION_UNREAD_CHANGED", false);
    }

    public static synchronized void clear() {
        notifications.clear();
        unread = false;
        AppEventBus.emit("NOTIFICATION_UNREAD_CHANGED", false);
    }
}
