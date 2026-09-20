package org.dmg.Dtos.Customer;

import java.math.BigDecimal;

public record OrderItemResponse(Long menuItemId,
                                String itemName,
                                BigDecimal unitPrice,
                                Integer quantity,
                                BigDecimal lineTotal) {
}

