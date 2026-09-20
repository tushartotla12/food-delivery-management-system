package org.dmg.Dtos.Admin;


import org.dmg.Entities.Enums.PartnerAvailabilityStatus;

public record DeliveryPartnerResponse(Long id,
                                      Long userId,
                                      Long cityId,
                                      PartnerAvailabilityStatus availabilityStatus,
                                      Boolean active) {
}

