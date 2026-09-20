package org.dmg.Dtos.Customer;

import org.dmg.Entities.Enums.RestaurantStatus;

public record RestaurantSummaryResponse(Long restaurantId,
                                        String name,
                                        String address,
                                        Long cityId,
                                        String cityName,
                                        RestaurantStatus status,
                                        Boolean active) {
}

