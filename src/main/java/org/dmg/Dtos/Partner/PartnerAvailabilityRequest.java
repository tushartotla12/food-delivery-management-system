package org.dmg.Dtos.Partner;

import org.dmg.Entities.Enums.PartnerAvailabilityStatus;

public record PartnerAvailabilityRequest(PartnerAvailabilityStatus availabilityStatus,
                                         Boolean active) {
}


