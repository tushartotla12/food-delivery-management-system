package org.dmg.Services;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.dmg.Dtos.Customer.*;
import org.dmg.Dtos.Owner.MenuItemRequest;
import org.dmg.Entities.*;
import org.dmg.Entities.Enums.AssignmentStatus;
import org.dmg.Entities.Enums.OrderStatus;
import org.dmg.Exception.BusinessRuleViolationException;
import org.dmg.Exception.ResourceNotFoundException;
import org.dmg.Repositories.*;
import org.dmg.Utilities.Util;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.Validator;

@Service
@RequiredArgsConstructor
public class OwnerService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final NotificationService notificationService;
    private final Util util;
    private final Validator validator;

    @Transactional
    public MenuItemResponse addMenuItem(Long ownerUserId, Long restaurantId, MenuItemRequest request) {
        Restaurant restaurant = ensureOwnedRestaurant(ownerUserId, restaurantId);
        Optional<MenuItem> optionalMenuItem = menuItemRepository.findByNameAndRestaurantId(request.name(),restaurantId);
        if(optionalMenuItem.isPresent())
        {
            if(request.name().equalsIgnoreCase(optionalMenuItem.get().getName())){
                throw new BusinessRuleViolationException("Menu Item with name already exists");
            }
        }
        MenuItem menuItem = MenuItem.builder()
                .restaurant(restaurant)
                .name(request.name())
                .description(trimToNull(request.description()))
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .available(request.available())
                .build();

        MenuItem saved = menuItemRepository.save(menuItem);
        return util.toMenuItemResponse(saved);
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long ownerUserId, Long restaurantId, Long menuItemId, MenuItemRequest request) {

        Set<ConstraintViolation<MenuItemRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantIdForUpdate(menuItemId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + menuItemId));
        ensureOwnedRestaurant(ownerUserId, restaurantId);

        if (StringUtils.hasText(request.name())) {
            menuItemRepository.findByNameAndRestaurantId(request.name(), restaurantId)
                    .ifPresent(existingItem -> {
                        if (!existingItem.getId().equals(menuItemId)) {
                            throw new BusinessRuleViolationException("Menu Item with name already exists");
                        }
                    });
            menuItem.setName(request.name());
        }
        if (request.description() != null) {
            menuItem.setDescription(trimToNull(request.description()));
        }
        if (request.price() != null) {
            menuItem.setPrice(requirePrice(request.price()));
        }
        if (request.stockQuantity() != null) {
            menuItem.setStockQuantity(requireStock(request.stockQuantity()));
        }
        if (request.available() != null) {
            menuItem.setAvailable(request.available());
        }
        return util.toMenuItemResponse(menuItemRepository.save(menuItem));
    }

    @Transactional
    public MenuItemResponse toggleMenuItemAvailability(Long ownerUserId, Long restaurantId, Long menuItemId, boolean available) {
        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantIdForUpdate(menuItemId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + menuItemId));
        ensureOwnedRestaurant(ownerUserId, restaurantId);
        menuItem.setAvailable(available);
        return util.toMenuItemResponse(menuItemRepository.save(menuItem));
    }

    @Transactional
    public OrderDetailsResponse acceptOrder(Long ownerUserId, Long restaurantId, Long orderId) {
        Restaurant restaurant = ensureOwnedRestaurant(ownerUserId, restaurantId);
        Order order = getOwnedOrder(ownerUserId, restaurantId, orderId);
        requireStatus(order, OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.REJECTED);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setAcceptedAt(LocalDateTime.now());
        OrderStatusHistory acceptHistory = addHistory(order, restaurant.getOwner(), OrderStatus.PLACED, OrderStatus.ACCEPTED, "Accepted by restaurant");
        Order saved = orderRepository.save(order);

        notificationService.publishOrderStatusChanged(
                notificationService.createNotificationFromOrder(saved, acceptHistory));

        return toOrderDetails(saved);
    }

    @Transactional
    public OrderDetailsResponse rejectOrder(Long ownerUserId, Long restaurantId, Long orderId, String reason) {
        Restaurant restaurant = ensureOwnedRestaurant(ownerUserId, restaurantId);
        Order order = getOwnedOrder(ownerUserId, restaurantId, orderId);
        requireStatus(order, OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.REJECTED);
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BusinessRuleViolationException("Only placed orders can be rejected in this simplified flow");
        }
        order.setStatus(OrderStatus.REJECTED);
        OrderStatusHistory rejectHistory = addHistory(order, restaurant.getOwner(), OrderStatus.PLACED, OrderStatus.REJECTED, normalizeReason(reason));
        Order saved = orderRepository.save(order);

        notificationService.publishOrderStatusChanged(
                notificationService.createNotificationFromOrder(saved, rejectHistory));

        return toOrderDetails(saved);
    }

    @Transactional
    public OrderDetailsResponse markPreparing(Long ownerUserId, Long restaurantId, Long orderId) {
        Restaurant restaurant = ensureOwnedRestaurant(ownerUserId, restaurantId);
        Order order = getOwnedOrder(ownerUserId, restaurantId, orderId);
        requireStatus(order, OrderStatus.ACCEPTED);
        order.setStatus(OrderStatus.PREPARING);
        OrderStatusHistory preparingHistory = addHistory(order, restaurant.getOwner(), OrderStatus.ACCEPTED, OrderStatus.PREPARING, "Kitchen started");
        Order saved = orderRepository.save(order);

        notificationService.publishOrderStatusChanged(
                notificationService.createNotificationFromOrder(saved, preparingHistory));

        return toOrderDetails(saved);
    }

    @Transactional
    public OrderDetailsResponse markReadyForPickup(Long ownerUserId, Long restaurantId, Long orderId) {
        Restaurant restaurant = ensureOwnedRestaurant(ownerUserId, restaurantId);
        Order order = getOwnedOrder(ownerUserId, restaurantId, orderId);
        requireStatus(order, OrderStatus.PREPARING);
        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        OrderStatusHistory readyHistory = addHistory(order, restaurant.getOwner(), OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP, "Ready for delivery pickup");

        if (order.getDeliveryAssignment() == null) {
            DeliveryAssignment assignment = DeliveryAssignment.builder()
            .order(order)
                    .status(AssignmentStatus.PENDING)
                    .assignedAt(LocalDateTime.now())
                    .build();
            order.setDeliveryAssignment(assignment);
            deliveryAssignmentRepository.save(assignment);
        }

        Order saved = orderRepository.save(order);
        notificationService.publishOrderStatusChanged(
                notificationService.createNotificationFromOrder(saved, readyHistory));

        return toOrderDetails(saved);
    }

    //util methods
    private Restaurant ensureOwnedRestaurant(Long ownerUserId, Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));
        if (restaurant.getOwner() == null || !restaurant.getOwner().getId().equals(ownerUserId)) {
            throw new BusinessRuleViolationException("Restaurant does not belong to this owner");
        }
        return restaurant;
    }

    private Order getOwnedOrder(Long ownerUserId, Long restaurantId, Long orderId) {
        ensureOwnedRestaurant(ownerUserId, restaurantId);
        return orderRepository.findByIdAndRestaurant_Id(orderId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private void requireStatus(Order order, OrderStatus... allowedStatuses) {
        for (OrderStatus allowedStatus : allowedStatuses) {
            if (order.getStatus() == allowedStatus) {
                return;
            }
        }
        throw new BusinessRuleViolationException("Invalid order state: " + order.getStatus());
    }

    private OrderStatusHistory addHistory(Order order, User changedBy, OrderStatus from, OrderStatus to, String remarks) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setRemarks(trimToNull(remarks));
        history.setChangedBy(changedBy);
        orderStatusHistoryRepository.save(history);
        order.getStatusHistory().add(history);
        return history;
    }

    private OrderDetailsResponse toOrderDetails(Order order) {
        List<OrderItemResponse> items = new ArrayList<>();
        for (OrderItem item : order.getOrderItems()) {
            items.add(new OrderItemResponse(
                    item.getMenuItem().getId(),
                    item.getItemNameSnapshot(),
                    item.getUnitPriceSnapshot(),
                    item.getQuantity(),
                    item.getLineTotal()
            ));
        }

        List<OrderTimelineEntryResponse> timeline = orderStatusHistoryRepository
                .findAllByOrder_IdOrderByChangedAtAsc(order.getId())
                .stream()
                .map(entry -> new OrderTimelineEntryResponse(
                        entry.getFromStatus(),
                        entry.getToStatus(),
                        entry.getChangedBy() == null ? null : entry.getChangedBy().getName(),
                        entry.getRemarks(),
                        entry.getChangedAt()))
                .toList();

        return new OrderDetailsResponse(
                order.getId(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                order.getDeliveryAddress(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getPlacedAt(),
                items,
                timeline);
    }


    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessRuleViolationException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeReason(String reason) {
        String normalized = trimToNull(reason);
        return normalized == null ? "Rejected by restaurant" : normalized;
    }

    private BigDecimal requirePrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new BusinessRuleViolationException("Price must be zero or positive");
        }
        return price;
    }

    private Integer requireStock(Integer stockQuantity) {
        if (stockQuantity == null || stockQuantity < 0) {
            throw new BusinessRuleViolationException("Stock quantity must be zero or positive");
        }
        return stockQuantity;
    }
}


