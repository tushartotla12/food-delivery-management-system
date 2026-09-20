package org.dmg.Dtos.Customer;

import java.math.BigDecimal;

public record MenuItemResponse(Long menuItemId,
                               Long restaurantId,
                               String name,
                               String description,
                               BigDecimal price,
                               Integer stockQuantity,
                               Boolean available) {
}

