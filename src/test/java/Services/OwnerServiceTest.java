package Services;

import org.dmg.Dtos.Customer.MenuItemResponse;
import org.dmg.Dtos.Customer.OrderDetailsResponse;
import org.dmg.Dtos.Customer.OrderTimelineEntryResponse;
import org.dmg.Dtos.Notification.OrderStatusNotification;
import org.dmg.Dtos.Owner.MenuItemRequest;
import org.dmg.Entities.Enums.*;
import org.dmg.Entities.*;
import org.dmg.Repositories.MenuItemRepository;
import org.dmg.Repositories.*;
import org.dmg.Services.NotificationService;
import org.dmg.Services.OwnerService;
import org.dmg.Utilities.Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Util util;

    @InjectMocks
    private OwnerService ownerService;

    @Test
    void addMenuItemTrimsAndPersistsItem() {
        User owner = new User();
        owner.setId(1L);
        owner.setName("Owner One");
        owner.setRole(Role.RESTAURANT_OWNER);
        owner.setActive(true);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(2L);
        restaurant.setName("Biryani House");
        restaurant.setAddress("Road 1");
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setActive(true);
        when(restaurantRepository.findById(2L)).thenReturn(Optional.of(restaurant));

        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
            MenuItem menuItem = invocation.getArgument(0);
            menuItem.setId(7L);
            return menuItem;
        });

        when(util.toMenuItemResponse(any(MenuItem.class))).thenAnswer(invocation -> {
            MenuItem menuItem = invocation.getArgument(0);

            return new MenuItemResponse(
                    menuItem.getId(),
                    menuItem.getRestaurant().getId(),
                    menuItem.getName(),
                    menuItem.getDescription(),
                    menuItem.getPrice(),
                    menuItem.getStockQuantity(),
                    menuItem.getAvailable()
            );
        });

        MenuItemResponse response = ownerService.addMenuItem(
                1L,
                2L,
                new MenuItemRequest("  Pizza  ", "  cheesy  ", new BigDecimal("12.50"), 8, null));

        assertThat(response).isEqualTo(new MenuItemResponse(
                7L,
                2L,
                "Pizza",
                "cheesy",
                new BigDecimal("12.50"),
                8,
                true));
    }

    @Test
    void acceptOrderMovesOrderToAcceptedAndPublishesNotification() {
        User owner = new User();
        owner.setId(1L);
        owner.setName("Owner One");
        owner.setRole(Role.RESTAURANT_OWNER);
        owner.setActive(true);

        City city = new City();
        city.setId(3L);
        city.setName("Hyderabad");

        Restaurant restaurant = new Restaurant();
        restaurant.setId(2L);
        restaurant.setName("Biryani House");
        restaurant.setAddress("Road 1");
        restaurant.setCity(city);
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setActive(true);

        Order order = new Order();
        order.setId(5L);
        order.setRestaurant(restaurant);
        order.setStatus(OrderStatus.PLACED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDeliveryAddress("221B Baker Street");
        order.setPlacedAt(LocalDateTime.of(2026, 9, 19, 10, 0));

        AtomicReference<OrderStatusHistory> savedHistory = new AtomicReference<>();
        LocalDateTime changedAt = LocalDateTime.of(2026, 9, 19, 10, 30);

        when(restaurantRepository.findById(2L)).thenReturn(Optional.of(restaurant));
        when(orderRepository.findByIdAndRestaurant_Id(5L, 2L)).thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class))).thenAnswer(invocation -> {
            OrderStatusHistory history = invocation.getArgument(0);
            history.setId(33L);
            history.setChangedAt(changedAt);
            savedHistory.set(history);
            return history;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderStatusHistoryRepository.findAllByOrder_IdOrderByChangedAtAsc(5L))
                .thenAnswer(invocation -> List.of(savedHistory.get()));
        when(notificationService.createNotificationFromOrder(any(Order.class), any(OrderStatusHistory.class)))
                .thenReturn(new OrderStatusNotification(
                        5L,
                        2L,
                        "Biryani House",
                        1L,
                        "Owner One",
                        9L,
                        "Customer One",
                        null,
                        null,
                        "221B Baker Street",
                        OrderStatus.PLACED,
                        OrderStatus.ACCEPTED,
                        "Accepted by restaurant",
                        changedAt,
                        "Owner One"));

        OrderDetailsResponse response = ownerService.acceptOrder(1L, 2L, 5L);

        assertThat(response.orderId()).isEqualTo(5L);
        assertThat(response.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(response.deliveryAddress()).isEqualTo("221B Baker Street");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(order.getAcceptedAt()).isNotNull();
        assertThat(response.timeline().get(0)).isEqualTo(new OrderTimelineEntryResponse(
                OrderStatus.PLACED,
                OrderStatus.ACCEPTED,
                "Owner One",
                "Accepted by restaurant",
                changedAt));
        verify(notificationService).publishOrderStatusChanged(any());
    }
}