package org.dmg.Repositories;

import org.dmg.Entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByCustomer_IdOrderByPlacedAtDesc(Long customerId);

    Optional<Order> findByIdAndCustomer_Id(Long orderId, Long customerId);

    Optional<Order> findByIdAndRestaurant_Id(Long orderId, Long restaurantId);
}

