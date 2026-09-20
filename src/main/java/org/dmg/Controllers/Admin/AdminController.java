package org.dmg.Controllers.Admin;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dmg.Dtos.Admin.*;
import org.dmg.Entities.City;
import org.dmg.Entities.DeliveryPartner;
import org.dmg.Entities.Restaurant;
import org.dmg.Entities.User;
import org.dmg.Services.AdminService;
import org.dmg.Utilities.Util;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final Util util;

    //create user api
    @PostMapping("/users")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return util.toUserResponse(adminService.createUser(request));
    }

    //manage city apis
    @PostMapping("/cities")
    public CityResponse createCity(@Valid @RequestBody CityRequest request) {
        return util.toCityResponse(adminService.createCity(request));
    }

    @PutMapping("/cities/{cityId}")
    public CityResponse updateCity(@PathVariable Long cityId, @RequestBody CityRequest request) {
        return util.toCityResponse(adminService.updateCity(cityId, request));
    }

    @PatchMapping("/cities/{cityId}/status")
    public CityResponse toggleCityActive(@PathVariable Long cityId, @RequestParam boolean active) {
        return util.toCityResponse(adminService.toggleCityActive(cityId, active));
    }

    //manage restaurants apis
    @PostMapping("/restaurants")
    public RestaurantResponse createRestaurant(@Valid @RequestBody RestaurantRequest request) {
        return util.toRestaurantResponse(adminService.createRestaurant(request));
    }

    @PutMapping("/restaurants/{restaurantId}")
    public RestaurantResponse updateRestaurant(@PathVariable Long restaurantId, @RequestBody RestaurantRequest request) {
        return util.toRestaurantResponse(adminService.updateRestaurant(restaurantId, request));
    }

    @PatchMapping("/restaurants/{restaurantId}/status")
    public RestaurantResponse toggleRestaurantActive(@PathVariable Long restaurantId, @RequestParam boolean active) {
        return util.toRestaurantResponse(adminService.toggleRestaurantActive(restaurantId, active));
    }

    //manage delivery partner apis
    @PostMapping("/delivery-partners")
    public DeliveryPartnerResponse createDeliveryPartner(@Valid @RequestBody DeliveryPartnerRequest request) {
        return util.toDeliveryPartnerResponse(adminService.createDeliveryPartner(request));
    }

    @PutMapping("/delivery-partners/{partnerId}")
    public DeliveryPartnerResponse updateDeliveryPartner(@PathVariable Long partnerId, @RequestBody DeliveryPartnerRequest request) {
        return util.toDeliveryPartnerResponse(adminService.updateDeliveryPartner(partnerId, request));
    }

    @PatchMapping("/delivery-partners/{partnerId}/status")
    public DeliveryPartnerResponse toggleDeliveryPartnerActive(@PathVariable Long partnerId, @RequestParam boolean active) {
        return util.toDeliveryPartnerResponse(adminService.toggleDeliveryPartnerActive(partnerId, active));
    }


    @GetMapping("/users")
    public List<UserResponse> getUsers() {
        return adminService.getUsers()
                .stream()
                .map(util::toUserResponse)
                .toList();
    }
}




