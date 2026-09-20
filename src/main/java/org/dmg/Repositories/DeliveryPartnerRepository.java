package org.dmg.Repositories;

import org.dmg.Entities.DeliveryPartner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, Long> {

    Optional<DeliveryPartner> findByUser_Id(Long userId);
}

