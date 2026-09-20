package org.dmg.Dtos.Customer;


import org.dmg.Entities.Enums.OrderStatus;
import org.dmg.Entities.Enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(Long orderId,
                                   Long restaurantId,
                                   String restaurantName,
                                   OrderStatus status,
                                   PaymentStatus paymentStatus,
                                   BigDecimal totalAmount,
                                   LocalDateTime placedAt) {
}

