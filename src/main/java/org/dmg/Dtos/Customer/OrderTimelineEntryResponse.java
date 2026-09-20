package org.dmg.Dtos.Customer;



import org.dmg.Entities.Enums.OrderStatus;

import java.time.LocalDateTime;

public record OrderTimelineEntryResponse(OrderStatus fromStatus,
                                         OrderStatus toStatus,
                                         String changedBy,
                                         String remarks,
                                         LocalDateTime changedAt) {
}

