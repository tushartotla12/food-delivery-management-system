package org.dmg.Dtos.Owner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record MenuItemRequest(
        @NotBlank(message = "Menu Item name is required")
        String name,
        String description,
        @NotNull(message = "Menu Item Price is required")
        @Positive(message = "Menu Item Price must be greater than 0")
        BigDecimal price,
        @PositiveOrZero(message = "Menu Item Quantity cannot be negative")
        @NotNull(message = "Menu Item Quantity is required")
        Integer stockQuantity,
        @NotNull(message = "Menu Item availability is required")
        Boolean available) {
}


