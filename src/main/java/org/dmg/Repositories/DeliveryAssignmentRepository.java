package org.dmg.Repositories;

import org.dmg.Entities.DeliveryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DeliveryAssignment d where d.order.id = :orderId")
    Optional<DeliveryAssignment> findByOrderIdForUpdate(@Param("orderId") Long orderId);
}

