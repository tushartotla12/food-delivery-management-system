package org.dmg.Dtos.Customer;

import org.dmg.Entities.Enums.OrderStatus;
import org.dmg.Entities.Enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailsResponse(Long orderId,
                                   Long restaurantId,
                                   String restaurantName,
                                   String deliveryAddress,
                                   OrderStatus status,
                                   PaymentStatus paymentStatus,
                                   BigDecimal totalAmount,
                                   LocalDateTime placedAt,
                                   List<OrderItemResponse> items,
                                   List<OrderTimelineEntryResponse> timeline) {
}

