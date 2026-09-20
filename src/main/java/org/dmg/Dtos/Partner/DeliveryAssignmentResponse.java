package org.dmg.Dtos.Partner;

import org.dmg.Entities.Enums.AssignmentStatus;

import java.time.LocalDateTime;

public record DeliveryAssignmentResponse(Long assignmentId,
                                         Long orderId,
                                         Long deliveryPartnerId,
                                         AssignmentStatus status,
                                         LocalDateTime assignedAt,
                                         LocalDateTime acceptedAt,
                                         LocalDateTime rejectedAt,
                                         LocalDateTime pickedUpAt,
                                         LocalDateTime deliveredAt) {
}

