package Services;

import org.dmg.Dtos.Customer.OrderDetailsResponse;
import org.dmg.Dtos.Customer.OrderItemRequest;
import org.dmg.Dtos.Customer.PlaceOrderRequest;
import org.dmg.Dtos.Customer.RestaurantSummaryResponse;
import org.dmg.Dtos.Customer.RatingReviewRequest;
import org.dmg.Dtos.Notification.OrderStatusNotification;
import org.dmg.Entities.Enums.*;
import org.dmg.Entities.*;
import org.dmg.Repositories.*;
import org.dmg.Services.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {


    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private RatingReviewRepository ratingReviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void browseRestaurantsByCityFiltersInactiveAndClosedRestaurants() {
        City city = new City();
        city.setId(1L);
        city.setName("Hyderabad");

        Restaurant activeRestaurant = new Restaurant();
        activeRestaurant.setId(10L);
        activeRestaurant.setName("Biryani House");
        activeRestaurant.setAddress("Road 1");
        activeRestaurant.setCity(city);
        activeRestaurant.setStatus(RestaurantStatus.OPEN);
        activeRestaurant.setActive(true);

        Restaurant inactiveRestaurant = new Restaurant();
        inactiveRestaurant.setId(11L);
        inactiveRestaurant.setName("Closed Kitchen");
        inactiveRestaurant.setAddress("Road 2");
        inactiveRestaurant.setCity(city);
        inactiveRestaurant.setStatus(RestaurantStatus.CLOSED);
        inactiveRestaurant.setActive(true);

        when(restaurantRepository.findAllByCity_Id(1L)).thenReturn(List.of(activeRestaurant, inactiveRestaurant));

        List<RestaurantSummaryResponse> restaurants = customerService.browseRestaurantsByCity(1L);

        assertThat(restaurants).hasSize(1);
        assertThat(restaurants.get(0)).isEqualTo(new RestaurantSummaryResponse(
                10L,
                "Biryani House",
                "Road 1",
                1L,
                "Hyderabad",
                RestaurantStatus.OPEN,
                true));
    }

    @Test
    void placeOrderCreatesOrderAndConsumesStock() {
        User customer = new User();
        customer.setId(1L);
        customer.setName("Customer One");
        customer.setRole(Role.CUSTOMER);
        customer.setActive(true);

        City city = new City();
        city.setId(2L);
        city.setName("Hyderabad");

        User owner = new User();
        owner.setId(3L);
        owner.setName("Owner One");
        owner.setRole(Role.RESTAURANT_OWNER);
        owner.setActive(true);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(4L);
        restaurant.setName("Biryani House");
        restaurant.setAddress("Road 1");
        restaurant.setCity(city);
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setActive(true);

        MenuItem menuItem = new MenuItem();
        menuItem.setId(5L);
        menuItem.setRestaurant(restaurant);
        menuItem.setName("Chicken Biryani");
        menuItem.setPrice(new BigDecimal("10.00"));
        menuItem.setStockQuantity(5);
        menuItem.setAvailable(true);

        AtomicReference<OrderStatusHistory> savedHistory = new AtomicReference<>();
        LocalDateTime changedAt = LocalDateTime.of(2026, 9, 19, 10, 15);

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(restaurantRepository.findById(4L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByIdAndRestaurantIdForUpdate(5L, 4L)).thenReturn(Optional.of(menuItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class))).thenAnswer(invocation -> {
            OrderStatusHistory history = invocation.getArgument(0);
            history.setId(90L);
            history.setChangedAt(changedAt);
            savedHistory.set(history);
            return history;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderStatusHistoryRepository.findAllByOrder_IdOrderByChangedAtAsc(100L))
                .thenAnswer(invocation -> List.of(savedHistory.get()));
        when(notificationService.createNotificationFromOrder(any(Order.class), any(OrderStatusHistory.class)))
                .thenReturn(new OrderStatusNotification(
                        100L,
                        4L,
                        "Biryani House",
                        3L,
                        "Owner One",
                        1L,
                        "Customer One",
                        null,
                        null,
                        "221B Baker Street",
                        null,
                        OrderStatus.PLACED,
                        "Order placed",
                        changedAt,
                        "Customer One"));

        OrderDetailsResponse details = customerService.placeOrder(
                1L,
                new PlaceOrderRequest(
                        4L,
                        "  221B Baker Street  ",
                        PaymentMethod.COD,
                        List.of(new OrderItemRequest(5L, 2))));

        assertThat(details.orderId()).isEqualTo(100L);
        assertThat(details.restaurantId()).isEqualTo(4L);
        assertThat(details.restaurantName()).isEqualTo("Biryani House");
        assertThat(details.deliveryAddress()).isEqualTo("221B Baker Street");
        assertThat(details.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(details.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(details.totalAmount()).isEqualByComparingTo("20.00");
        assertThat(details.items()).hasSize(1);
        assertThat(details.items().get(0).menuItemId()).isEqualTo(5L);
        assertThat(menuItem.getStockQuantity()).isEqualTo(3);
        verify(notificationService).publishOrderStatusChanged(any());
    }

    @Test
    void capturePaymentMarksOrderAndPaymentCapturedBeforeDelivery() {
        User customer = new User();
        customer.setId(1L);
        customer.setName("Customer One");
        customer.setRole(Role.CUSTOMER);
        customer.setActive(true);

        City city = new City();
        city.setId(2L);
        city.setName("Hyderabad");

        User owner = new User();
        owner.setId(3L);
        owner.setName("Owner One");
        owner.setRole(Role.RESTAURANT_OWNER);
        owner.setActive(true);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(4L);
        restaurant.setName("Biryani House");
        restaurant.setAddress("Road 1");
        restaurant.setCity(city);
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setActive(true);

        Order order = new Order();
        order.setId(100L);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setCity(city);
        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDeliveryAddress("221B Baker Street");
        order.setTotalAmount(new BigDecimal("20.00"));
        order.setPlacedAt(LocalDateTime.of(2026, 9, 19, 10, 0));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(new BigDecimal("20.00"));
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setStatus(PaymentStatus.PENDING);
        order.setPayment(payment);

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.findByIdAndCustomer_Id(100L, 1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderStatusHistoryRepository.findAllByOrder_IdOrderByChangedAtAsc(100L)).thenReturn(List.of());

        OrderDetailsResponse details = customerService.capturePayment(1L, 100L);

        assertThat(details.orderId()).isEqualTo(100L);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(payment.getPaidAt()).isNotNull();
        verify(paymentRepository).save(payment);
    }

//    @Test
//    void cancelOrderRefundsPaymentState() {
//        User customer = new User();
//        customer.setId(1L);
//        customer.setName("Customer One");
//        customer.setRole(Role.CUSTOMER);
//        customer.setActive(true);
//
//        City city = new City();
//        city.setId(2L);
//        city.setName("Hyderabad");
//
//        User owner = new User();
//        owner.setId(3L);
//        owner.setName("Owner One");
//        owner.setRole(Role.RESTAURANT_OWNER);
//        owner.setActive(true);
//
//        Restaurant restaurant = new Restaurant();
//        restaurant.setId(4L);
//        restaurant.setName("Biryani House");
//        restaurant.setAddress("Road 1");
//        restaurant.setCity(city);
//        restaurant.setOwner(owner);
//        restaurant.setStatus(RestaurantStatus.OPEN);
//        restaurant.setActive(true);
//
//        Order order = new Order();
//        order.setId(101L);
//        order.setCustomer(customer);
//        order.setRestaurant(restaurant);
//        order.setCity(city);
//        order.setStatus(OrderStatus.PLACED);
//        order.setPaymentStatus(PaymentStatus.CAPTURED);
//        order.setDeliveryAddress("221B Baker Street");
//        order.setTotalAmount(new BigDecimal("20.00"));
//        order.setPlacedAt(LocalDateTime.of(2026, 9, 19, 10, 0));
//
//        Payment payment = new Payment();
//        payment.setOrder(order);
//        payment.setAmount(new BigDecimal("20.00"));
//        payment.setPaymentMethod(PaymentMethod.CARD);
//        payment.setStatus(PaymentStatus.CAPTURED);
//        order.setPayment(payment);
//
//        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
//        when(orderRepository.findByIdAndCustomer_Id(101L, 1L)).thenReturn(Optional.of(order));
//        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
//        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
//        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
//        when(orderStatusHistoryRepository.findAllByOrder_IdOrderByChangedAtAsc(101L)).thenReturn(List.of());
//
//        OrderDetailsResponse details = customerService.cancelOrder(1L, 101L);
//
//        assertThat(details.orderId()).isEqualTo(101L);
//        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
//        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
//        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
//        verify(paymentRepository).save(payment);
//    }

    @Test
    void submitReviewCreatesReviewOnlyAfterDelivery() {
        User customer = new User();
        customer.setId(1L);
        customer.setName("Customer One");
        customer.setRole(Role.CUSTOMER);
        customer.setActive(true);

        City city = new City();
        city.setId(2L);
        city.setName("Hyderabad");

        User owner = new User();
        owner.setId(3L);
        owner.setName("Owner One");
        owner.setRole(Role.RESTAURANT_OWNER);
        owner.setActive(true);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(4L);
        restaurant.setName("Biryani House");
        restaurant.setAddress("Road 1");
        restaurant.setCity(city);
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setActive(true);

        Order order = new Order();
        order.setId(5L);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setCity(city);
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDeliveryAddress("221B Baker Street");
        order.setTotalAmount(new BigDecimal("20.00"));
        order.setPlacedAt(LocalDateTime.of(2026, 9, 19, 10, 0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.findByIdAndCustomer_Id(5L, 1L)).thenReturn(Optional.of(order));
        when(ratingReviewRepository.existsByOrder_Id(5L)).thenReturn(false);
        when(ratingReviewRepository.save(any(RatingReview.class))).thenAnswer(invocation -> {
            RatingReview review = invocation.getArgument(0);
            review.setId(77L);
            return review;
        });

        RatingReview review = customerService.submitReview(1L, 5L, new RatingReviewRequest(5, 4, "  great  "));

        assertThat(review.getId()).isEqualTo(77L);
        assertThat(review.getOrder().getId()).isEqualTo(5L);
        assertThat(review.getRestaurantRating()).isEqualTo(5);
        assertThat(review.getPartnerRating()).isEqualTo(4);
        assertThat(review.getReviewComment()).isEqualTo("great");
        verify(ratingReviewRepository).save(any(RatingReview.class));
    }
}

