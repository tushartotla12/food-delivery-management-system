package org.dmg.Services;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dmg.Dtos.Notification.OrderStatusNotification;
import org.dmg.Entities.Order;
import org.dmg.Entities.OrderStatusHistory;
import org.dmg.Events.OrderStatusChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notification Service
 * Handles asynchronous notifications for order status changes.
 * All methods are executed in a separate thread pool (async) to prevent blocking the main flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Send notification to customer asynchronously.
     * The customer is notified about order status changes.
     *
     * @param notification Order status notification containing order details
     */
    public void publishOrderStatusChanged(OrderStatusNotification notification) {
        log.info("Publishing order status notification event for orderId={}, fromStatus={}, toStatus={}",
                notification.orderId(), notification.previousStatus(), notification.currentStatus());
        eventPublisher.publishEvent(new OrderStatusChangedEvent(notification));
    }

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyCustomerAfterCommit(OrderStatusChangedEvent event) {
        notifyCustomer(event.notification());
    }

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyRestaurantAfterCommit(OrderStatusChangedEvent event) {
        notifyRestaurant(event.notification());
    }

    @Async("asyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyDeliveryPartnerAfterCommit(OrderStatusChangedEvent event) {
        OrderStatusNotification notification = event.notification();
        if (!shouldNotifyDeliveryPartner(notification)) {
            log.info("Skipping delivery partner notification for orderId={} because no assigned partner is available for status={}",
                    notification.orderId(), notification.currentStatus());
            return;
        }
        notifyDeliveryPartner(notification);
    }

    private void notifyCustomer(OrderStatusNotification notification) {
        try {
            log.info("Notifying CUSTOMER - Order ID: {}, Status: {} -> {}, Customer ID: {}",
                    notification.orderId(), notification.previousStatus(), notification.currentStatus(), notification.customerId());

            String message = String.format(
                    "Your order #%d from %s is now %s.",
                    notification.orderId(),
                    notification.restaurantName(),
                    notification.currentStatus()
            );

            log.info("CUSTOMER Notification Content: {}", message);
            log.info("CUSTOMER Notification sent successfully for order: {}", notification.orderId());
        } catch (Exception e) {
            log.error("Error sending CUSTOMER notification for order: {}", notification.orderId(), e);
        }
    }

    /**
     * Send notification to restaurant asynchronously.
     * The restaurant owner is notified about payment and delivery status.
     *
     * @param notification Order status notification containing order details
     */
    private void notifyRestaurant(OrderStatusNotification notification) {
        try {
            log.info("Notifying RESTAURANT - Order ID: {}, Status: {} -> {}, Restaurant ID: {}",
                    notification.orderId(), notification.previousStatus(), notification.currentStatus(), notification.restaurantId());

            String message = String.format(
                    "Order #%d for restaurant %s is now %s. Customer: %s",
                    notification.orderId(),
                    notification.restaurantName(),
                    notification.currentStatus(),
                    notification.customerName()
            );

            log.info("RESTAURANT Notification Content: {}", message);
            log.info("RESTAURANT Notification sent successfully for order: {}", notification.orderId());
        } catch (Exception e) {
            log.error("Error sending RESTAURANT notification for order: {}", notification.orderId(), e);
        }
    }

    /**
     * Send notification to delivery partner asynchronously.
     * The delivery partner is notified about pickup and delivery assignments.
     *
     * @param notification Order status notification containing order details
     */
    private void notifyDeliveryPartner(OrderStatusNotification notification) {
        try {
            log.info("Notifying DELIVERY PARTNER - Order ID: {}, Status: {} -> {}, Partner User ID: {}",
                    notification.orderId(), notification.previousStatus(), notification.currentStatus(), notification.deliveryPartnerUserId());

            String message = String.format(
                    "Order #%d from %s is now %s. Deliver to: %s",
                    notification.orderId(),
                    notification.restaurantName(),
                    notification.currentStatus(),
                    notification.deliveryAddress()
            );

            log.info("DELIVERY PARTNER Notification Content: {}", message);
            log.info("DELIVERY PARTNER Notification sent successfully for order: {}", notification.orderId());
        } catch (Exception e) {
            log.error("Error sending DELIVERY PARTNER notification for order: {}", notification.orderId(), e);
        }
    }

    private boolean shouldNotifyDeliveryPartner(OrderStatusNotification notification) {
        if (notification.deliveryPartnerUserId() == null) {
            return false;
        }

        return switch (notification.currentStatus()) {
            case PICKED_UP, OUT_FOR_DELIVERY, DELIVERED, CANCELLED -> true;
            default -> false;
        };
    }

    /**
     * Create OrderStatusNotification from Order entity and status history entry
     *
     * @param order The order entity
     * @param statusHistory The status history entry containing the status change details
     * @return OrderStatusNotification DTO
     */
    public OrderStatusNotification createNotificationFromOrder(Order order, OrderStatusHistory statusHistory) {
        Long deliveryPartnerUserId = null;
        String deliveryPartnerName = null;
        if (order.getDeliveryAssignment() != null && order.getDeliveryAssignment().getDeliveryPartner() != null
                && order.getDeliveryAssignment().getDeliveryPartner().getUser() != null) {
            deliveryPartnerUserId = order.getDeliveryAssignment().getDeliveryPartner().getUser().getId();
            deliveryPartnerName = order.getDeliveryAssignment().getDeliveryPartner().getUser().getName();
        }

        return new OrderStatusNotification(
                order.getId(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                order.getRestaurant().getOwner() == null ? null : order.getRestaurant().getOwner().getId(),
                order.getRestaurant().getOwner() == null ? null : order.getRestaurant().getOwner().getName(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                deliveryPartnerUserId,
                deliveryPartnerName,
                order.getDeliveryAddress(),
                statusHistory.getFromStatus(),
                statusHistory.getToStatus(),
                statusHistory.getRemarks(),
                statusHistory.getChangedAt(),
                statusHistory.getChangedBy() != null ? statusHistory.getChangedBy().getName() : "System"
        );
    }
}




