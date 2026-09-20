package org.dmg.Dtos.Admin;


import org.dmg.Entities.Enums.RestaurantStatus;

public record RestaurantResponse(Long id,
                                 String name,
                                 String address,
                                 Long cityId,
                                 Long ownerUserId,
                                 RestaurantStatus status,
                                 Boolean active) {
}

