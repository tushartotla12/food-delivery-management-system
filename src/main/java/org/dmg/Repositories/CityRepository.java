package org.dmg.Repositories;

import org.dmg.Entities.City;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {

    boolean existsByName(String name);
}

