package org.dmg.Dtos.Admin;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.dmg.Entities.Enums.RestaurantStatus;

public record RestaurantRequest(
        @NotBlank(message = "Restaurant name is required")
        String name,
        @NotBlank(message = "Address is required")
        String address,
        @NotNull(message = "CityId is required")
        Long cityId,
        @NotNull(message = "OwnerId is required")
        Long ownerUserId,
        @NotNull(message = "Restaurant status is required")
        RestaurantStatus status,
        Boolean active) {
}


