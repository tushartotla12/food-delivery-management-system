package org.dmg.Services;


import lombok.RequiredArgsConstructor;
import org.dmg.Dtos.Partner.PartnerAvailabilityRequest;
import org.dmg.Entities.*;
import org.dmg.Entities.Enums.*;
import org.dmg.Exception.BusinessRuleViolationException;
import org.dmg.Exception.ResourceNotFoundException;
import org.dmg.Repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PartnerService {

    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<DeliveryAssignment> getPendingAssignments(Long partnerUserId) {
        DeliveryPartner partner = getPartner(partnerUserId);
        return deliveryAssignmentRepository.findAll().stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.PENDING)
                .filter(assignment -> assignment.getOrder() != null)
                .filter(assignment -> assignment.getOrder().getRestaurant() != null)
                .filter(assignment -> partner.getCity().getId().equals(assignment.getOrder().getRestaurant().getCity().getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliveryAssignment> getActiveAssignments(Long partnerUserId) {
        DeliveryPartner partner = getPartner(partnerUserId);
        return deliveryAssignmentRepository.findAll().stream()
                .filter(assignment -> assignment.getDeliveryPartner() != null)
                .filter(assignment -> assignment.getDeliveryPartner().getId().equals(partner.getId()))
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.ACCEPTED || assignment.getStatus() == AssignmentStatus.COMPLETED)
                .toList();
    }

    @Transactional
    public DeliveryAssignment acceptAssignment(Long partnerUserId, Long orderId) {
        DeliveryPartner partner = getPartner(partnerUserId);
        if (partner.getAvailabilityStatus() == PartnerAvailabilityStatus.BUSY) {
            throw new BusinessRuleViolationException("Partner is currently busy");
        }
        DeliveryAssignment assignment = getLockedAssignment(orderId);
        ensurePending(assignment);
        assignment.setDeliveryPartner(partner);
        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignment.setAcceptedAt(LocalDateTime.now());
        partner.setAvailabilityStatus(PartnerAvailabilityStatus.BUSY);
        deliveryPartnerRepository.save(partner);
        return deliveryAssignmentRepository.save(assignment);
    }

    @Transactional
    public DeliveryAssignment rejectAssignment(Long partnerUserId, Long orderId) {
        DeliveryPartner partner = getPartner(partnerUserId);
        DeliveryAssignment assignment = getLockedAssignment(orderId);
        ensurePending(assignment);
        assignment.setDeliveryPartner(partner);
        assignment.setStatus(AssignmentStatus.REJECTED);
        assignment.setRejectedAt(LocalDateTime.now());
        partner.setAvailabilityStatus(PartnerAvailabilityStatus.AVAILABLE);
        deliveryPartnerRepository.save(partner);
        return deliveryAssignmentRepository.save(assignment);
    }

    @Transactional
    public Order markPickedUp(Long partnerUserId, Long orderId) {
        DeliveryPartner partner = getPartner(partnerUserId);
        DeliveryAssignment assignment = getPartnerAssignmentForUpdate(partner, orderId);
        ensureAssignmentStatus(assignment, AssignmentStatus.ACCEPTED);
        Order order = assignment.getOrder();
        ensureOrderStatus(order, OrderStatus.READY_FOR_PICKUP);
        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignment.setPickedUpAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PICKED_UP);
        OrderStatusHistory pickedUpHistory = addHistory(order, partner.getUser(), OrderStatus.READY_FOR_PICKUP, OrderStatus.PICKED_UP, partner.getUser().getName() + " picked up the order");
        deliveryAssignmentRepository.save(assignment);
        Order saved = orderRepository.save(order);

        notificationService.publishOrderStatusChanged(
                notificationService.createNotificationFromOrder(saved, pickedUpHistory));

        return saved;
    }

    @Transactional
    public Order markOutForDelivery(Long partnerUserId, Long orderId) {
        DeliveryPartner partner = getPartner(partnerUserId);
        DeliveryAssignment assignment = getPartnerAssignmentForUpdate(partner, orderId);
        ensureOrderStatus(assignment.getOrder(), OrderStatus.PICKED_UP);
        assignment.setDeliveredAt(null);
        assignment.setStatus(AssignmentStatus.ACCEPTED);
        Order order = assignment.getOrder();
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        OrderStatusHistory outForDeliveryHistory = addHistory(order, partner.getUser(), OrderStatus.PICKED_UP, OrderStatus.OUT_FOR_DELIVERY, partner.getUser().getName() + " is out for delivery");
        deliveryAssignmentRepository.save(assignment);
        Order saved = orderRepository.save(order);

        notificationService.publishOrderStatusChanged(
                notificationService.createNotificationFromOrder(saved, outForDeliveryHistory));

        return saved;
    }

    @Transactional
    public Order markDelivered(Long partnerUserId, Long orderId) {
        DeliveryPartner partner = getPartner(partnerUserId);
        DeliveryAssignment assignment = getPartnerAssignmentForUpdate(partner, orderId);
        ensureOrderStatus(assignment.getOrder(), OrderStatus.OUT_FOR_DELIVERY);
        Order order = assignment.getOrder();
        if (order.getPayment() == null
                || order.getPaymentStatus() != PaymentStatus.CAPTURED
                || order.getPayment().getStatus() != PaymentStatus.CAPTURED) {
            throw new BusinessRuleViolationException("Payment must be captured before delivery");
        }
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setDeliveredAt(LocalDateTime.now());
        partner.setAvailabilityStatus(PartnerAvailabilityStatus.AVAILABLE);
        OrderStatusHistory deliveredHistory = addHistory(order, partner.getUser(), OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED, partner.getUser().getName() + " delivered the order");
        deliveryPartnerRepository.save(partner);
        deliveryAssignmentRepository.save(assignment);
        Order saved = orderRepository.save(order);

        notificationService.publishOrderStatusChanged(
                notificationService.createNotificationFromOrder(saved, deliveredHistory));

        return saved;
    }

    @Transactional
    public DeliveryPartner updateAvailability(Long partnerUserId, PartnerAvailabilityRequest request) {
        DeliveryPartner partner = getPartner(partnerUserId);
        if (request.availabilityStatus() != null) {
            partner.setAvailabilityStatus(request.availabilityStatus());
        }
        if (request.active() != null) {
            partner.setActive(request.active());
        }
        return deliveryPartnerRepository.save(partner);
    }

    private DeliveryPartner getPartner(Long partnerUserId) {
        User user = deliveryPartnerRepository.findByUser_Id(partnerUserId)
                .map(DeliveryPartner::getUser)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found for user: " + partnerUserId));
        if (user.getRole() != Role.DELIVERY_PARTNER) {
            throw new BusinessRuleViolationException("Delivery partner role required");
        }
        return deliveryPartnerRepository.findByUser_Id(partnerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found for user: " + partnerUserId));
    }

    private DeliveryAssignment getLockedAssignment(Long orderId) {
        return deliveryAssignmentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery assignment not found for order: " + orderId));
    }

    private DeliveryAssignment getPartnerAssignmentForUpdate(DeliveryPartner partner, Long orderId) {
        DeliveryAssignment assignment = getLockedAssignment(orderId);
        if (assignment.getDeliveryPartner() == null || !assignment.getDeliveryPartner().getId().equals(partner.getId())) {
            throw new BusinessRuleViolationException("Assignment does not belong to this partner");
        }
        return assignment;
    }

    private void ensurePending(DeliveryAssignment assignment) {
        if (assignment.getStatus() != AssignmentStatus.PENDING) {
            throw new BusinessRuleViolationException("Assignment is not pending");
        }
    }

    private void ensureAssignmentStatus(DeliveryAssignment assignment, AssignmentStatus expectedStatus) {
        if (assignment.getStatus() != expectedStatus) {
            throw new BusinessRuleViolationException("Invalid assignment state: " + assignment.getStatus());
        }
    }

    private void ensureOrderStatus(Order order, OrderStatus expectedStatus) {
        if (order.getStatus() != expectedStatus) {
            throw new BusinessRuleViolationException("Invalid order state: " + order.getStatus());
        }
    }

    private OrderStatusHistory addHistory(Order order, User changedBy, OrderStatus from, OrderStatus to, String remarks) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setRemarks(remarks);
        history.setChangedBy(changedBy);
        orderStatusHistoryRepository.save(history);
        order.getStatusHistory().add(history);
        return history;
    }
}


