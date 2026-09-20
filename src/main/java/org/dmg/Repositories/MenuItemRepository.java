package org.dmg.Repositories;

import org.dmg.Entities.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    Optional<MenuItem> findByNameAndRestaurantId(String name,Long restaurantId);

    List<MenuItem> findAllByRestaurant_IdAndAvailableTrue(Long restaurantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MenuItem m where m.id = :id and m.restaurant.id = :restaurantId")
    Optional<MenuItem> findByIdAndRestaurantIdForUpdate(@Param("id") Long id,
                                                        @Param("restaurantId") Long restaurantId);
}

