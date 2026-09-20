package org.dmg.Services;


import lombok.RequiredArgsConstructor;
import org.dmg.Dtos.Customer.*;
import org.dmg.Entities.*;
import org.dmg.Entities.Enums.Role;
import org.dmg.Entities.Enums.OrderStatus;
import org.dmg.Entities.Enums.PaymentMethod;
import org.dmg.Entities.Enums.PaymentStatus;
import org.dmg.Entities.Enums.RestaurantStatus;
import org.dmg.Exception.BusinessRuleViolationException;
import org.dmg.Exception.ConflictException;
import org.dmg.Exception.ResourceNotFoundException;
import org.dmg.Repositories.*;
import org.dmg.Utilities.Util;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CityRepository cityRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final RatingReviewRepository ratingReviewRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Util util;

    @Transactional(readOnly = true)
    public List<City> listCities() {
        return cityRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<RestaurantSummaryResponse> browseRestaurantsByCity(Long cityId) {
        return restaurantRepository.findAllByCity_Id(cityId).stream()
                .filter(Restaurant::getActive)
                .filter(restaurant -> restaurant.getStatus() == RestaurantStatus.OPEN)
                .map(this::toRestaurantSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> browseMenu(Long restaurantId) {
        return menuItemRepository.findAllByRestaurant_IdAndAvailableTrue(restaurantId).stream()
                .map(util::toMenuItemResponse)
                .toList();
    }

//        @Transactional
//        public OrderDetailsResponse placeOrder(Long customerUserId, PlaceOrderRequest request) {
//            User customer = getCustomer(customerUserId);
//            Restaurant restaurant = getActiveRestaurant(request.restaurantId());
//            List<OrderItemRequest> requestedItems = request.items();
//
//            Map<Long, Integer> requestedQuantities = aggregateRequestedQuantities(requestedItems);
//            List<OrderItem> orderItems = new ArrayList<>();
//            BigDecimal totalAmount = BigDecimal.ZERO;
//
//            for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
//                MenuItem menuItem = menuItemRepository.findByIdAndRestaurantIdForUpdate(entry.getKey(), restaurant.getId())
//                        .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + entry.getKey()));
//
//                int quantity = entry.getValue();
//                if (menuItem.getStockQuantity() < quantity) {
//                    throw new BusinessRuleViolationException("Insufficient stock for menu item: " + menuItem.getName());
//                }
//
//                menuItem.setStockQuantity(menuItem.getStockQuantity() - quantity);
//                menuItemRepository.save(menuItem);
//
//                OrderItem orderItem = OrderItem.builder()
//                        .menuItem(menuItem)
//                        .quantity(quantity)
//                        .itemNameSnapshot(menuItem.getName())
//                        .unitPriceSnapshot(menuItem.getPrice())
//                        .lineTotal(menuItem.getPrice().multiply(BigDecimal.valueOf(quantity)))
//                        .build();
//
//                orderItems.add(orderItem);
//
//                totalAmount = totalAmount.add(orderItem.getLineTotal());
//            }
//
//            Order order = Order.builder()
//                    .customer(customer)
//                    .restaurant(restaurant)
//                    .city(restaurant.getCity())
//                    .deliveryAddress(requireText(
//                            request.deliveryAddress(),
//                            "Delivery address is required"
//                    ))
//                    .status(OrderStatus.PLACED)
//                    .paymentStatus(PaymentStatus.PENDING)
//                    .totalAmount(totalAmount)
//                    .placedAt(LocalDateTime.now())
//                    .orderItems(orderItems)
//                    .build();
//
//            for (OrderItem orderItem : orderItems) {
//                orderItem.setOrder(order);
//            }
//
//            Payment payment = new Payment();
//            payment.setOrder(order);
//            payment.setAmount(totalAmount);
//            payment.setPaymentMethod(request.paymentMethod() == null ? PaymentMethod.COD : request.paymentMethod());
//            payment.setStatus(PaymentStatus.PENDING);
//            order.setPayment(payment);
//
//            OrderStatusHistory placedHistory = addHistory(order, null, OrderStatus.PLACED, "Order placed");
//
//            Order saved = orderRepository.save(order);
//            paymentRepository.save(payment);
//
//            notificationService.publishOrderStatusChanged(
//                    notificationService.createNotificationFromOrder(saved, placedHistory));
//
//            return toDetails(saved);
//        }
@Transactional
public OrderDetailsResponse placeOrder(Long customerUserId, PlaceOrderRequest request) {

    User customer = getCustomer(customerUserId);
    Restaurant restaurant = getActiveRestaurant(request.restaurantId());
    List<OrderItemRequest> requestedItems = request.items();

    Map<Long, Integer> requestedQuantities =
            aggregateRequestedQuantities(requestedItems);

    List<OrderItem> orderItems = new ArrayList<>();
    BigDecimal totalAmount = BigDecimal.ZERO;

    for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {

        MenuItem menuItem = menuItemRepository
                .findByIdAndRestaurantIdForUpdate(
                        entry.getKey(),
                        restaurant.getId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Menu item not found: " + entry.getKey()
                        )
                );

        int quantity = entry.getValue();

        if (menuItem.getStockQuantity() < quantity) {
            throw new BusinessRuleViolationException(
                    "Insufficient stock for menu item: " + menuItem.getName()
            );
        }

        menuItem.setStockQuantity(
                menuItem.getStockQuantity() - quantity
        );

        menuItemRepository.save(menuItem);

        OrderItem orderItem = OrderItem.builder()
                .menuItem(menuItem)
                .quantity(quantity)
                .itemNameSnapshot(menuItem.getName())
                .unitPriceSnapshot(menuItem.getPrice())
                .lineTotal(
                        menuItem.getPrice()
                                .multiply(BigDecimal.valueOf(quantity))
                )
                .build();

        orderItems.add(orderItem);

        totalAmount = totalAmount.add(orderItem.getLineTotal());
    }

    Order order = Order.builder()
            .customer(customer)
            .restaurant(restaurant)
            .city(restaurant.getCity())
            .deliveryAddress(requireText(request.deliveryAddress(), "Delivery address is required"))
            .status(OrderStatus.PLACED)
            .paymentStatus(PaymentStatus.PENDING)
            .totalAmount(totalAmount)
            .placedAt(LocalDateTime.now())
            .orderItems(orderItems)
            .build();

    for (OrderItem orderItem : orderItems) {
        orderItem.setOrder(order);
    }

    // Create Payment
    Payment payment = new Payment();
    payment.setOrder(order);
    payment.setAmount(totalAmount);
    payment.setPaymentMethod(
            request.paymentMethod() == null
                    ? PaymentMethod.COD
                    : request.paymentMethod()
    );
    payment.setStatus(PaymentStatus.PENDING);

    order.setPayment(payment);

    //NOT calling orderStatusHistoryRepository.save() here.
    OrderStatusHistory placedHistory = addHistory(
            order,
            null,
            OrderStatus.PLACED,
            "Order placed"
    );

    Order saved = orderRepository.save(order);

    notificationService.publishOrderStatusChanged(
            notificationService.createNotificationFromOrder(
                    saved,
                    placedHistory
            )
    );

    return toDetails(saved);
}


    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getOrders(Long customerUserId) {
        getCustomer(customerUserId);
        return orderRepository.findAllByCustomer_IdOrderByPlacedAtDesc(customerUserId)
                .stream().map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailsResponse getOrderDetails(Long customerUserId, Long orderId) {
        Order order = getOwnedOrder(customerUserId, orderId);
        return toDetails(order);
    }

    @Transactional
    public RatingReview submitReview(Long customerUserId, Long orderId, RatingReviewRequest request) {
        Order order = getOwnedOrder(customerUserId, orderId);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessRuleViolationException("Review can be submitted only after delivery");
        }
        if (ratingReviewRepository.existsByOrder_Id(orderId)) {
            throw new ConflictException("Review already exists for order: " + orderId);
        }
        if (request.restaurantRating() <= 0 && request.restaurantRating() >10) {
            throw new ConflictException("Rating can be between 1 to 10");
        }
        if (request.partnerRating() <= 0 && request.partnerRating() >10) {
            throw new ConflictException("Rating can be between 1 to 10");
        }

        RatingReview review = RatingReview.builder()
                .order(order)
                .customer(order.getCustomer())
                .restaurantRating(request.restaurantRating())
                .partnerRating(request.partnerRating())
                .reviewComment(request.reviewComment())
                .build();
        return ratingReviewRepository.save(review);
    }

    //util methods

    private Restaurant getActiveRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));
        if (!Boolean.TRUE.equals(restaurant.getActive()) || restaurant.getStatus() != RestaurantStatus.OPEN) {
            throw new BusinessRuleViolationException("Restaurant is not accepting orders");
        }
        return restaurant;
    }

    private User getCustomer(Long customerUserId) {
        User customer = userRepository.findById(customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + customerUserId));
        if (customer.getRole() != Role.CUSTOMER) {
            throw new BusinessRuleViolationException("Customer role required");
        }
        return customer;
    }

    private Order getOwnedOrder(Long customerUserId, Long orderId) {
        getCustomer(customerUserId);
        return orderRepository.findByIdAndCustomer_Id(orderId, customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private Map<Long, Integer> aggregateRequestedQuantities(List<OrderItemRequest> requestedItems) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest item : requestedItems) {
            if (item.menuItemId() == null) {
                throw new BusinessRuleViolationException("Menu item id is required");
            }
            if (item.quantity() == null || item.quantity() < 1) {
                throw new BusinessRuleViolationException("Quantity must be at least 1");
            }
            quantities.merge(item.menuItemId(), item.quantity(), Integer::sum);
        }
        return quantities;
    }

    private OrderStatusHistory addHistory(
            Order order,
            OrderStatus fromStatus,
            OrderStatus toStatus,
            String remarks
    ) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .remarks(remarks)
                .changedAt(LocalDateTime.now())
                .build();

        order.addStatusHistory(history);

        return history;
    }


    private OrderDetailsResponse toDetails(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getMenuItem().getId(),
                        item.getItemNameSnapshot(),
                        item.getUnitPriceSnapshot(),
                        item.getQuantity(),
                        item.getLineTotal()))
                .toList();

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

    private OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getPlacedAt());
    }

    private RestaurantSummaryResponse toRestaurantSummary(Restaurant restaurant) {
        City city = restaurant.getCity();
        return new RestaurantSummaryResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getAddress(),
                city == null ? null : city.getId(),
                city == null ? null : city.getName(),
                restaurant.getStatus(),
                restaurant.getActive());
    }

    private MenuItemResponse toMenuItemResponse(MenuItem menuItem) {
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getRestaurant().getId(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getPrice(),
                menuItem.getStockQuantity(),
                menuItem.getAvailable());
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

    @Transactional
    public OrderDetailsResponse cancelOrder(Long customerUserId, Long orderId) {
        Order order = getOwnedOrder(customerUserId, orderId);
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BusinessRuleViolationException("Only placed orders can be cancelled in this simplified flow");
        }
        order.setStatus(OrderStatus.CANCELLED);
        OrderStatusHistory cancelHistory = addHistory(order, OrderStatus.PLACED, OrderStatus.CANCELLED, "Cancelled by customer");
        Order saved = orderRepository.save(order);

        notificationService.publishOrderStatusChanged(
                notificationService.createNotificationFromOrder(saved, cancelHistory));

        return toDetails(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderTimelineEntryResponse> getOrderTimeline(Long customerUserId, Long orderId) {
        getOwnedOrder(customerUserId, orderId);
        return orderStatusHistoryRepository.findAllByOrder_IdOrderByChangedAtAsc(orderId).stream()
                .map(entry -> new OrderTimelineEntryResponse(
                        entry.getFromStatus(),
                        entry.getToStatus(),
                        entry.getChangedBy() == null ? null : entry.getChangedBy().getName(),
                        entry.getRemarks(),
                        entry.getChangedAt()))
                .toList();
    }

    @Transactional
    public OrderDetailsResponse capturePayment(Long customerUserId, Long orderId) {
        Order order = getOwnedOrder(customerUserId, orderId);
        if (order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.REJECTED
                || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessRuleViolationException("Payment cannot  be captured for " + order.getStatus() + " orders");
        }

        Payment payment = order.getPayment();
        if (payment == null) {
            throw new BusinessRuleViolationException("Payment record not found for order: " + orderId);
        }

        order.setPaymentStatus(PaymentStatus.CAPTURED);
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
        paymentRepository.save(payment);

        return toDetails(order);
    }
}

