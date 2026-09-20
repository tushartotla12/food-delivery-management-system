package org.dmg.Controllers.Owner;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dmg.Dtos.Customer.MenuItemResponse;
import org.dmg.Dtos.Customer.OrderDetailsResponse;
import org.dmg.Dtos.Owner.MenuItemRequest;
import org.dmg.Security.UserPrincipal;
import org.dmg.Services.OwnerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class OwnerController {

    private final OwnerService ownerService;

    @PostMapping("/restaurants/{restaurantId}/menu-items")
    public MenuItemResponse addMenuItem(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long restaurantId,
                                        @Valid @RequestBody MenuItemRequest request) {
        return ownerService.addMenuItem(principal.getId(), restaurantId, request);
    }

    @PutMapping("/restaurants/{restaurantId}/menu-items/{menuItemId}")
    public MenuItemResponse updateMenuItem(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long restaurantId,
                                           @PathVariable Long menuItemId,
                                           @RequestBody MenuItemRequest request) {
        return ownerService.updateMenuItem(principal.getId(), restaurantId, menuItemId, request);
    }

    @PatchMapping("/restaurants/{restaurantId}/menu-items/{menuItemId}/availability")
    public MenuItemResponse toggleMenuItemAvailability(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable Long restaurantId,
                                                        @PathVariable Long menuItemId,
                                                        @RequestParam boolean available) {
        return ownerService.toggleMenuItemAvailability(principal.getId(), restaurantId, menuItemId, available);
    }

    @PatchMapping("/restaurants/{restaurantId}/orders/{orderId}/accept")
    public OrderDetailsResponse acceptOrder(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable Long restaurantId,
                                            @PathVariable Long orderId) {
        return ownerService.acceptOrder(principal.getId(), restaurantId, orderId);
    }

    @PatchMapping("/restaurants/{restaurantId}/orders/{orderId}/reject")
    public OrderDetailsResponse rejectOrder(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long restaurantId,
                                           @PathVariable Long orderId,
                                           @RequestParam(required = false) String reason) {
        return ownerService.rejectOrder(principal.getId(), restaurantId, orderId, reason);
    }

    @PatchMapping("/restaurants/{restaurantId}/orders/{orderId}/prepare")
    public OrderDetailsResponse markPreparing(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long restaurantId,
                                              @PathVariable Long orderId) {
        return ownerService.markPreparing(principal.getId(), restaurantId, orderId);
    }

    @PatchMapping("/restaurants/{restaurantId}/orders/{orderId}/ready-for-pickup")
    public OrderDetailsResponse markReadyForPickup(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long restaurantId,
                                                   @PathVariable Long orderId) {
        return ownerService.markReadyForPickup(principal.getId(), restaurantId, orderId);
    }
}

