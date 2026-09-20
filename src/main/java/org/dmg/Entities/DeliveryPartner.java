package org.dmg.Entities;

import jakarta.persistence.*;
import lombok.*;
import org.dmg.Entities.Base.AuditableEntity;
import org.dmg.Entities.Enums.PartnerAvailabilityStatus;

import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "delivery_partners")
public class DeliveryPartner extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    private PartnerAvailabilityStatus availabilityStatus = PartnerAvailabilityStatus.AVAILABLE;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "deliveryPartner", fetch = FetchType.LAZY)
    private List<DeliveryAssignment> assignments = new ArrayList<>();
}
