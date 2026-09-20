package org.dmg.Utilities;

import org.dmg.Dtos.Admin.CityResponse;
import org.dmg.Dtos.Admin.DeliveryPartnerResponse;
import org.dmg.Dtos.Admin.RestaurantResponse;
import org.dmg.Dtos.Admin.UserResponse;
import org.dmg.Dtos.Customer.*;
import org.dmg.Dtos.Partner.DeliveryAssignmentResponse;
import org.dmg.Entities.*;
import org.springframework.stereotype.Component;


@Component
public class Util {
    public CityResponse toCityResponse(City city) {
        return new CityResponse(city.getId(), city.getName(), city.getActive());
    }

    public RestaurantResponse toRestaurantResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getAddress(),
                restaurant.getCity() == null ? null : restaurant.getCity().getId(),
                restaurant.getOwner() == null ? null : restaurant.getOwner().getId(),
                restaurant.getStatus(),
                restaurant.getActive());
    }

    public DeliveryPartnerResponse toDeliveryPartnerResponse(DeliveryPartner partner) {
        return new DeliveryPartnerResponse(
                partner.getId(),
                partner.getUser() == null ? null : partner.getUser().getId(),
                partner.getCity() == null ? null : partner.getCity().getId(),
                partner.getAvailabilityStatus(),
                partner.getActive());
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getActive());
    }


    public MenuItemResponse toMenuItemResponse(MenuItem menuItem) {
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getRestaurant().getId(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getPrice(),
                menuItem.getStockQuantity(),
                menuItem.getAvailable());
    }

    public DeliveryAssignmentResponse toDeliveryAssignmentResponse(DeliveryAssignment assignment) {
        return new DeliveryAssignmentResponse(
                assignment.getId(),
                assignment.getOrder() == null ? null : assignment.getOrder().getId(),
                assignment.getDeliveryPartner() == null ? null : assignment.getDeliveryPartner().getId(),
                assignment.getStatus(),
                assignment.getAssignedAt(),
                assignment.getAcceptedAt(),
                assignment.getRejectedAt(),
                assignment.getPickedUpAt(),
                assignment.getDeliveredAt());
    }

    public  OrderSummaryResponse toOrderSummary(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getPlacedAt());
    }

}
