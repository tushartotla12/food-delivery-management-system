package org.dmg.Entities;


import jakarta.persistence.*;
import lombok.*;
import org.dmg.Entities.Base.AuditableEntity;
import org.dmg.Entities.Enums.Role;

import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users" )
public class User extends AuditableEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false,unique = true,length = 100)
    private String email;

    @Column(nullable = false,unique = true, length = 20)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    private List<Restaurant> ownedRestaurants = new ArrayList<>();

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<Order> customerOrders = new ArrayList<>();

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private DeliveryPartner deliveryPartnerProfile;

    @OneToMany(mappedBy = "changedBy", fetch = FetchType.LAZY)
    private List<OrderStatusHistory> orderStatusUpdates = new ArrayList<>();

    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<RatingReview> reviews = new ArrayList<>();
}