package org.dmg.Controllers.Partner;


import lombok.RequiredArgsConstructor;
import org.dmg.Dtos.Admin.DeliveryPartnerResponse;
import org.dmg.Dtos.Customer.OrderSummaryResponse;
import org.dmg.Dtos.Partner.DeliveryAssignmentResponse;
import org.dmg.Dtos.Partner.PartnerAvailabilityRequest;
import org.dmg.Entities.DeliveryAssignment;
import org.dmg.Entities.DeliveryPartner;
import org.dmg.Entities.Order;
import org.dmg.Security.UserPrincipal;
import org.dmg.Services.PartnerService;
import org.dmg.Utilities.Util;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DELIVERY_PARTNER')")
public class PartnerController {

    private final PartnerService partnerService;
    private final Util util;

    @PatchMapping("/assignments/{orderId}/accept")
    public DeliveryAssignmentResponse acceptAssignment(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable Long orderId) {
        return util.toDeliveryAssignmentResponse(partnerService.acceptAssignment(principal.getId(), orderId));
    }

    @PatchMapping("/assignments/{orderId}/reject")
    public DeliveryAssignmentResponse rejectAssignment(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable Long orderId) {
        return util.toDeliveryAssignmentResponse(partnerService.rejectAssignment(principal.getId(), orderId));
    }

    @PatchMapping("/orders/{orderId}/picked-up")
    public OrderSummaryResponse markPickedUp(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long orderId) {
        return util.toOrderSummary(partnerService.markPickedUp(principal.getId(), orderId));
    }

    @PatchMapping("/orders/{orderId}/out-for-delivery")
    public OrderSummaryResponse markOutForDelivery(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long orderId) {
        return util.toOrderSummary(partnerService.markOutForDelivery(principal.getId(), orderId));
    }

    @PatchMapping("/orders/{orderId}/delivered")
    public OrderSummaryResponse markDelivered(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long orderId) {
        return util.toOrderSummary(partnerService.markDelivered(principal.getId(), orderId));
    }

    @PatchMapping("/me/availability")
    public DeliveryPartnerResponse updateAvailability(@AuthenticationPrincipal UserPrincipal principal,
                                                      @RequestBody PartnerAvailabilityRequest request) {
        DeliveryPartner partner = partnerService.updateAvailability(principal.getId(), request);
        return new DeliveryPartnerResponse(
                partner.getId(),
                partner.getUser() == null ? null : partner.getUser().getId(),
                partner.getCity() == null ? null : partner.getCity().getId(),
                partner.getAvailabilityStatus(),
                partner.getActive());
    }


    // can include in admin to check all assignments for a partner,
    // but for now just the pending and active ones for the partner themselves
    // to verify what all are pending or active  at their side

    @GetMapping("/assignments/pending")
    public List<DeliveryAssignmentResponse> pendingAssignments(@AuthenticationPrincipal UserPrincipal principal) {
        return partnerService.getPendingAssignments(principal.getId()).stream().map(util::toDeliveryAssignmentResponse).toList();
    }

    @GetMapping("/assignments/active")
    public List<DeliveryAssignmentResponse> activeAssignments(@AuthenticationPrincipal UserPrincipal principal) {
        return partnerService.getActiveAssignments(principal.getId()).stream().map(util::toDeliveryAssignmentResponse).toList();
    }
}


