package org.dmg.Controllers.Customer;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dmg.Dtos.Admin.CityResponse;
import org.dmg.Dtos.Customer.*;
import org.dmg.Entities.RatingReview;
import org.dmg.Services.CustomerService;
import org.dmg.Utilities.Util;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.dmg.Security.UserPrincipal;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private final CustomerService customerService;
    private final Util util;

    @GetMapping("/cities")
    public List<CityResponse> listCities() {
        return customerService.listCities().stream().map(util::toCityResponse).toList();
    }

    @GetMapping("/restaurants")
    public List<RestaurantSummaryResponse> browseRestaurants(@RequestParam Long cityId) {
        return customerService.browseRestaurantsByCity(cityId);
    }

    @GetMapping("/restaurants/{restaurantId}/menu")
    public List<MenuItemResponse> browseMenu(@PathVariable Long restaurantId) {
        return customerService.browseMenu(restaurantId);
    }

    @PostMapping("/orders")
    public OrderDetailsResponse placeOrder(@AuthenticationPrincipal UserPrincipal principal,
                                           @Valid @RequestBody PlaceOrderRequest request) {
        return customerService.placeOrder(principal.getId(), request);
    }

    @PatchMapping("/orders/{orderId}/cancel")
    public OrderDetailsResponse cancelOrder(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable Long orderId) {
        return customerService.cancelOrder(principal.getId(), orderId);
    }

    @GetMapping("/orders")
    public List<OrderSummaryResponse> getAllOrders(@AuthenticationPrincipal UserPrincipal principal) {
        return customerService.getOrders(principal.getId());
    }

    @GetMapping("/orders/{orderId}")
    public OrderDetailsResponse getOrderDetails(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable Long orderId) {
        return customerService.getOrderDetails(principal.getId(), orderId);
    }

    @PostMapping("/orders/{orderId}/rating")
    public RatingReviewResponse submitReview(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long orderId,
                                     @RequestBody RatingReviewRequest request) {
        RatingReview review = customerService.submitReview(principal.getId(), orderId, request);
        return new RatingReviewResponse(
                review.getId(),
                review.getOrder().getId(),
                review.getCustomer().getId(),
                review.getRestaurantRating(),
                review.getPartnerRating(),
                review.getReviewComment());
    }

    @PatchMapping("/orders/{orderId}/payment")
    public OrderDetailsResponse capturePayment(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable Long orderId) {
        return customerService.capturePayment(principal.getId(), orderId);
    }

    @GetMapping("/orders/{orderId}/timeline")
    public List<OrderTimelineEntryResponse> getOrderTimeline(@AuthenticationPrincipal UserPrincipal principal,
                                                             @PathVariable Long orderId) {
        return customerService.getOrderTimeline(principal.getId(), orderId);
    }


}


