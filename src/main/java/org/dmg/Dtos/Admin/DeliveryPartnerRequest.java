package org.dmg.Dtos.Admin;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.dmg.Entities.Enums.PartnerAvailabilityStatus;

public record DeliveryPartnerRequest(
        @NotNull(message = "UserId is required")
        Long userId,
        @NotNull(message = "CityId is required")
        Long cityId,
        @NotNull(message = "Availability Status is required")
        PartnerAvailabilityStatus availabilityStatus,
        Boolean active) {
}


