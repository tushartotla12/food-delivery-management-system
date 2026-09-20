package Services;

import org.dmg.Dtos.Notification.OrderStatusNotification;
import org.dmg.Entities.DeliveryAssignment;
import org.dmg.Entities.DeliveryPartner;
import org.dmg.Entities.Enums.*;
import org.dmg.Entities.*;
import org.dmg.Entities.OrderStatusHistory;
import org.dmg.Exception.BusinessRuleViolationException;
import org.dmg.Repositories.*;
import org.dmg.Services.NotificationService;
import org.dmg.Services.PartnerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerServiceTest {

    @Mock
    private DeliveryPartnerRepository deliveryPartnerRepository;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PartnerService partnerService;

    @Test
    void acceptAssignmentMarksPartnerBusyAndAssignmentAccepted() {
        DeliveryPartner partner = buildPartner(PartnerAvailabilityStatus.AVAILABLE);
        DeliveryAssignment assignment = buildAssignment(partner, AssignmentStatus.PENDING, OrderStatus.READY_FOR_PICKUP);

        when(deliveryPartnerRepository.findByUser_Id(1L)).thenAnswer(invocation -> Optional.of(partner));
        when(deliveryAssignmentRepository.findByOrderIdForUpdate(10L)).thenReturn(Optional.of(assignment));
        when(deliveryPartnerRepository.save(any(DeliveryPartner.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeliveryAssignment result = partnerService.acceptAssignment(1L, 10L);

        assertThat(result.getStatus()).isEqualTo(AssignmentStatus.ACCEPTED);
        assertThat(result.getDeliveryPartner()).isEqualTo(partner);
        assertThat(result.getAcceptedAt()).isNotNull();
        assertThat(partner.getAvailabilityStatus()).isEqualTo(PartnerAvailabilityStatus.BUSY);
        verify(deliveryPartnerRepository).save(partner);
        verify(deliveryAssignmentRepository).save(assignment);
    }

    @Test
    void markDeliveredCompletesOrderAndReleasesPartner() {
        DeliveryPartner partner = buildPartner(PartnerAvailabilityStatus.BUSY);
        DeliveryAssignment assignment =
                buildAssignment(partner, AssignmentStatus.ACCEPTED, OrderStatus.OUT_FOR_DELIVERY);

        Order order = assignment.getOrder();

        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.CAPTURED);

        order.setPayment(payment);
        order.setPaymentStatus(PaymentStatus.CAPTURED);

        LocalDateTime changedAt = LocalDateTime.of(2026, 9, 19, 11, 0);

        when(deliveryPartnerRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(partner));
        when(deliveryAssignmentRepository.findByOrderIdForUpdate(10L))
                .thenReturn(Optional.of(assignment));

        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class)))
                .thenAnswer(invocation -> {
                    OrderStatusHistory history = invocation.getArgument(0);
                    history.setChangedAt(changedAt);
                    return history;
                });

        when(deliveryPartnerRepository.save(any(DeliveryPartner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(notificationService.createNotificationFromOrder(
                any(Order.class),
                any(OrderStatusHistory.class)))
                .thenReturn(new OrderStatusNotification(
                        10L,
                        20L,
                        "Biryani House",
                        30L,
                        "Owner One",
                        40L,
                        "Customer One",
                        1L,
                        "Partner One",
                        "221B Baker Street",
                        OrderStatus.OUT_FOR_DELIVERY,
                        OrderStatus.DELIVERED,
                        "Partner delivered the order",
                        changedAt,
                        "Partner One"));

        Order result = partnerService.markDelivered(1L, 10L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(result.getDeliveredAt()).isNotNull();
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
        assertThat(assignment.getDeliveredAt()).isNotNull();
        assertThat(partner.getAvailabilityStatus())
                .isEqualTo(PartnerAvailabilityStatus.AVAILABLE);

        verify(notificationService).publishOrderStatusChanged(any());
    }


    @Test
    void markOutForDeliveryMovesCapturedOrderToDeliveryState() {
        DeliveryPartner partner = buildPartner(PartnerAvailabilityStatus.BUSY);
        DeliveryAssignment assignment = buildAssignment(partner, AssignmentStatus.ACCEPTED, OrderStatus.PICKED_UP);
        Payment payment = new Payment();
        payment.setOrder(assignment.getOrder());
        payment.setAmount(assignment.getOrder().getTotalAmount());
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setStatus(PaymentStatus.CAPTURED);
        assignment.getOrder().setPayment(payment);

        when(deliveryPartnerRepository.findByUser_Id(1L)).thenAnswer(invocation -> Optional.of(partner));
        when(deliveryAssignmentRepository.findByOrderIdForUpdate(10L)).thenReturn(Optional.of(assignment));
        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationService.createNotificationFromOrder(any(Order.class), any(OrderStatusHistory.class)))
                .thenReturn(new OrderStatusNotification(
                        10L,
                        20L,
                        "Biryani House",
                        30L,
                        "Owner One",
                        40L,
                        "Customer One",
                        1L,
                        "Partner One",
                        "221B Baker Street",
                        OrderStatus.PICKED_UP,
                        OrderStatus.OUT_FOR_DELIVERY,
                        "Partner is out for delivery",
                        LocalDateTime.of(2026, 9, 19, 11, 30),
                        "Partner One"));

        Order result = partnerService.markOutForDelivery(1L, 10L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
        verify(notificationService).publishOrderStatusChanged(any());
    }

    private DeliveryPartner buildPartner(PartnerAvailabilityStatus availabilityStatus) {
        User user = new User();
        user.setId(1L);
        user.setName("Partner One");
        user.setRole(Role.DELIVERY_PARTNER);
        user.setActive(true);

        City city = new City();
        city.setId(2L);
        city.setName("Hyderabad");

        DeliveryPartner partner = new DeliveryPartner();
        partner.setId(5L);
        partner.setUser(user);
        partner.setCity(city);
        partner.setAvailabilityStatus(availabilityStatus);
        partner.setActive(true);
        return partner;
    }

    private DeliveryAssignment buildAssignment(DeliveryPartner partner, AssignmentStatus status, OrderStatus orderStatus) {
        User owner = new User();
        owner.setId(30L);
        owner.setName("Owner One");
        owner.setRole(Role.RESTAURANT_OWNER);
        owner.setActive(true);

        User customer = new User();
        customer.setId(40L);
        customer.setName("Customer One");
        customer.setRole(Role.CUSTOMER);
        customer.setActive(true);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(20L);
        restaurant.setName("Biryani House");
        restaurant.setAddress("Road 1");
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setActive(true);

        City city = new City();
        city.setId(2L);
        city.setName("Hyderabad");
        restaurant.setCity(city);

        Order order = new Order();
        order.setId(10L);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setCity(city);
        order.setStatus(orderStatus);
        order.setPaymentStatus(PaymentStatus.CAPTURED);
        order.setTotalAmount(new java.math.BigDecimal("20.00"));
        order.setDeliveryAddress("221B Baker Street");
        order.setPlacedAt(LocalDateTime.of(2026, 9, 19, 10, 0));

        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setId(90L);
        assignment.setOrder(order);
        assignment.setDeliveryPartner(partner);
        assignment.setStatus(status);
        assignment.setAssignedAt(LocalDateTime.of(2026, 9, 19, 10, 5));
        order.setDeliveryAssignment(assignment);
        return assignment;
    }
}
