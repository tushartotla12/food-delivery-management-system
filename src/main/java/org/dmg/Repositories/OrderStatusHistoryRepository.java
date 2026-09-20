package org.dmg.Repositories;

import org.dmg.Entities.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findAllByOrder_IdOrderByChangedAtAsc(Long orderId);
}

