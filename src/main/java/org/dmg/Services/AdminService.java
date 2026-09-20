package org.dmg.Services;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.dmg.Dtos.Admin.CityRequest;
import org.dmg.Dtos.Admin.CreateUserRequest;
import org.dmg.Dtos.Admin.DeliveryPartnerRequest;
import org.dmg.Dtos.Admin.RestaurantRequest;
import org.dmg.Entities.City;
import org.dmg.Entities.DeliveryPartner;
import org.dmg.Entities.Enums.PartnerAvailabilityStatus;
import org.dmg.Entities.Enums.RestaurantStatus;
import org.dmg.Entities.Enums.Role;
import org.dmg.Entities.Restaurant;
import org.dmg.Entities.User;
import org.dmg.Exception.BusinessRuleViolationException;
import org.dmg.Exception.ConflictException;
import org.dmg.Exception.ResourceNotFoundException;
import org.dmg.Repositories.CityRepository;
import org.dmg.Repositories.DeliveryPartnerRepository;
import org.dmg.Repositories.RestaurantRepository;
import org.dmg.Repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Builder
@Service
@RequiredArgsConstructor
public class AdminService {

    private final CityRepository cityRepository;
    private final RestaurantRepository restaurantRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(CreateUserRequest request) {
        String name = request.name().trim();
        String email = request.email().trim();
        String phone = request.phone().trim();
        String password = request.password().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("User email already exists: " + email);
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ConflictException("User phone already exists: " + phone);
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .password(passwordEncoder.encode(password))
                .role(request.role())
                .active(request.active() == null || request.active()).build();
        return userRepository.save(user);
    }

    @Transactional
    public City createCity(CityRequest request) {
        String name = request.name().trim();
        if (cityRepository.existsByName(name)) {
            throw new ConflictException("City already exists: " + name);
        }
        City city = City.builder()
                .name(name)
                .active(request.active() == null || request.active())
                .build();
        return cityRepository.save(city);
    }

    @Transactional
    public City updateCity(Long cityId, CityRequest request) {
        City city = getCity(cityId);
        if(request.name()!=null)
        {
            String name = request.name().trim();
            if (!city.getName().equalsIgnoreCase(name) && cityRepository.existsByName(name)) {
                throw new ConflictException("City already exists: " + name);
            }
            city.setName(name);
        }
        if (request.active() != null) {
            city.setActive(request.active());
        }
        return cityRepository.save(city);
    }

    @Transactional
    public City toggleCityActive(Long cityId, boolean active) {
        City city = getCity(cityId);
        city.setActive(active);
        return cityRepository.save(city);
    }

    @Transactional(readOnly = true)
    public City getCity(Long cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + cityId));
    }


    @Transactional
    public Restaurant createRestaurant(RestaurantRequest request) {
        City city = getCity(request.cityId());
        User owner = getUser(request.ownerUserId());
        assertRole(owner, Role.RESTAURANT_OWNER, "Restaurant owner");

        Restaurant restaurant = Restaurant.builder()
                .name(normalize(request.name()))
                .address(normalize(request.address()))
                .city(city)
                .owner(owner)
                .status(request.status())
                .active(request.active() == null || request.active())
                .build();
        return restaurantRepository.save(restaurant);
    }

    @Transactional
    public Restaurant updateRestaurant(Long restaurantId, RestaurantRequest request) {
        Restaurant restaurant = getRestaurant(restaurantId);

        if (StringUtils.hasText(request.name())) {
            restaurant.setName(normalize(request.name()));
        }
        if (StringUtils.hasText(request.address())) {
            restaurant.setAddress(normalize(request.address()));
        }
        if (request.cityId() != null) {
            restaurant.setCity(getCity(request.cityId()));
        }
        if (request.ownerUserId() != null) {
            User owner = getUser(request.ownerUserId());
            assertRole(owner, Role.RESTAURANT_OWNER, "Restaurant owner");
            restaurant.setOwner(owner);
        }
        if (request.status() != null) {
            restaurant.setStatus(request.status());
        }
        if (request.active() != null) {
            restaurant.setActive(request.active());
        }
        return restaurantRepository.save(restaurant);
    }


    @Transactional
    public Restaurant toggleRestaurantActive(Long restaurantId, boolean active) {
        Restaurant restaurant = getRestaurant(restaurantId);
        restaurant.setActive(active);
        return restaurantRepository.save(restaurant);
    }

    @Transactional
    public DeliveryPartner createDeliveryPartner(DeliveryPartnerRequest request) {
        User user = getUser(request.userId());
        assertRole(user, Role.DELIVERY_PARTNER, "Delivery partner");
        City city = getCity(request.cityId());

        if (deliveryPartnerRepository.findByUser_Id(user.getId()).isPresent()) {
            throw new ConflictException("Delivery partner already exists for user: " + user.getId());
        }

        DeliveryPartner partner = DeliveryPartner.builder()
                .user(user)
                .city(city)
                .availabilityStatus(request.availabilityStatus())
                .active(request.active() == null || request.active())
                .build();
        return deliveryPartnerRepository.save(partner);
    }

    @Transactional
    public DeliveryPartner updateDeliveryPartner(Long partnerId, DeliveryPartnerRequest request) {
        DeliveryPartner partner = getDeliveryPartner(partnerId);
        if (request.cityId() != null) {
            partner.setCity(getCity(request.cityId()));
        }
        if (request.availabilityStatus() != null) {
            partner.setAvailabilityStatus(request.availabilityStatus());
        }
        if (request.active() != null) {
            partner.setActive(request.active());
        }
        return deliveryPartnerRepository.save(partner);
    }

    @Transactional
    public DeliveryPartner toggleDeliveryPartnerActive(Long partnerId, boolean active) {
        DeliveryPartner partner = getDeliveryPartner(partnerId);
        partner.setActive(active);
        return deliveryPartnerRepository.save(partner);
    }


    @Transactional(readOnly = true)
    public DeliveryPartner getDeliveryPartner(Long partnerId) {
        return deliveryPartnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found: " + partnerId));
    }

    //util methods
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Restaurant getRestaurant(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));
    }

    private void assertRole(User user, Role expectedRole, String label) {
        if (user.getRole() != expectedRole) {
            throw new BusinessRuleViolationException(label + " role required");
        }
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessRuleViolationException(message);
        }
        return value.trim();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }
}

