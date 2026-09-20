package org.dmg.Events;

import org.dmg.Dtos.Notification.OrderStatusNotification;

public record OrderStatusChangedEvent(OrderStatusNotification notification) {
}

