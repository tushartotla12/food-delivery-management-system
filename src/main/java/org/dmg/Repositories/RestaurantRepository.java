package org.dmg.Repositories;

import org.dmg.Entities.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findAllByCity_Id(Long cityId);
}

