package org.dmg.Dtos.Notification;

import org.dmg.Entities.Enums.OrderStatus;
import java.time.LocalDateTime;

/**
 * Generic order status notification DTO
 * This is sent to all stakeholders (customer, restaurant, delivery partner)
 */
public record OrderStatusNotification(
        Long orderId,
        Long restaurantId,
        String restaurantName,
        Long restaurantOwnerUserId,
        String restaurantOwnerName,
        Long customerId,
        String customerName,
        Long deliveryPartnerUserId,
        String deliveryPartnerName,
        String deliveryAddress,
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String remarks,
        LocalDateTime changedAt,
        String changedBy
) {
}


