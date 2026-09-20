package org.dmg.Dtos.Customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.dmg.Entities.Enums.PaymentMethod;

import java.util.List;


public record PlaceOrderRequest(
        @NotNull(message = "Restaurant Id is required")
        @Positive(message = "Restaurant Id must be greater than 0")
        Long restaurantId,

        @NotBlank(message = "Delivery address is required")
        String deliveryAddress,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @NotEmpty(message = "Order must contain at least one item")
        List<@Valid OrderItemRequest> items
) {
}



